"""
claw_runtime.py

Claw 语言 -> Python 目标的完整运行时支持

包含:
  1. 异常类型体系 (ClawException 及子类)
  2. 属性变更监听系统 (@BeforeProps / @AfterProps)
  3. 构造/析构钩子管理 (@BeforeName / @AfterName)
  4. 业务流转支持 (flow-to)
  5. 类型辅助
  6. 字符串辅助

使用方式:
  from claw_runtime import *
  或
  import claw_runtime as cr
"""

from __future__ import annotations
import weakref
import functools
import inspect
import threading
from typing import Any, Callable, Dict, List, Optional, Tuple, Set


# ================================================================
#  1. 异常类型体系
#
#  Claw 的 exception 流映射为 Python 原生异常继承树
#  Claw 去掉了 try 和 {}，但 Python 本身就有 try/except
#  编译器生成的 Python 代码直接使用 try/except
#  这里定义 Claw 特有的异常类型
# ================================================================

class ClawException(Exception):
    """Claw 基础异常"""
    def __init__(self, message: str = "", context: Any = None):
        super().__init__(message)
        self.message = message
        self.context = context


class ClawRuntimeError(ClawException):
    """运行时错误"""
    pass


class ClawLogicError(ClawException):
    """逻辑错误"""
    pass


# --- I/O 相关 ---

class IOError_(ClawException):
    """I/O 错误（避免与内置 IOError 冲突，内部用 IOError_ ）"""
    pass

# 同时注册别名，让生成的代码可以用 ClawIOError
ClawIOError = IOError_


class FileError(ClawException):
    """文件操作错误"""
    pass


class NetworkError(ClawException):
    """网络错误"""
    pass


# --- 数据验证 ---

class ValidationError(ClawException):
    """数据验证错误"""
    pass


class TypeError_(ClawException):
    """类型错误（避免与内置 TypeError 冲突）"""
    pass

ClawTypeError = TypeError_


class RangeError(ClawException):
    """范围越界"""
    pass


class NullError(ClawException):
    """空值引用"""
    pass


class IndexError_(ClawException):
    """索引越界（避免与内置 IndexError 冲突）"""
    pass

ClawIndexError = IndexError_


# --- 业务逻辑 ---

class PaymentError(ClawException):
    """支付错误"""
    pass


class AuthError(ClawException):
    """认证错误"""
    pass


class PermissionError_(ClawException):
    """权限错误"""
    pass

ClawPermissionError = PermissionError_


# --- 内存 ---

class OutOfMemoryError(ClawException):
    """内存不足"""
    pass


class AllocError(ClawException):
    """分配错误"""
    pass


# --- 格式错误 ---

class FormatError(ClawException):
    """格式化错误"""
    pass


# 异常名称 -> 类 的映射表（编译器生成代码时查表用）
EXCEPTION_MAP: Dict[str, type] = {
    "Exception":        ClawException,
    "RuntimeError":     ClawRuntimeError,
    "LogicError":       ClawLogicError,
    "IOError":          ClawIOError,
    "FileError":        FileError,
    "NetworkError":     NetworkError,
    "ValidationError":  ValidationError,
    "TypeError":        ClawTypeError,
    "RangeError":       RangeError,
    "NullError":        NullError,
    "IndexError":       ClawIndexError,
    "PaymentError":     PaymentError,
    "AuthError":        AuthError,
    "PermissionError":  ClawPermissionError,
    "OutOfMemoryError": OutOfMemoryError,
    "AllocError":       AllocError,
    "FormatError":      FormatError,
}


def get_exception_class(name: str) -> type:
    """根据异常名获取对应的异常类"""
    return EXCEPTION_MAP.get(name, ClawException)


# ================================================================
#  2. 业务流转支持 (flow-to)
#
#  Claw 的 flow-to 语义：不记录堆栈，直接向后向上跳转
#  在 Python 中用特殊异常 + 装饰器实现
# ================================================================

class FlowJump(BaseException):
    """
    业务流转跳转

    继承 BaseException 而非 Exception，
    这样普通的 except Exception 不会误捕获它
    """
    def __init__(self, target: str, data: Any = None):
        self.target = target
        self.data = data
        super().__init__(f"flow to {target}")


def flow_to(target: str, data: Any = None):
    """
    触发业务流转跳转

    Claw 代码:  flow to approvalFlow
    生成 Python: claw_runtime.flow_to("approvalFlow")
    """
    raise FlowJump(target, data)


def flow_handler(func: Callable) -> Callable:
    """
    装饰器：为函数添加 flow-to 跳转目标支持

    使用方式（编译器自动生成）:

        @claw_runtime.flow_handler
        def processOrder(order):
            # 正常逻辑
            if need_approval:
                claw_runtime.flow_to("approvalFlow")

            return True

            # flow 目标由编译器转换为函数内的处理块
    """
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except FlowJump as fj:
            # 查找当前函数中注册的 flow 目标处理器
            handler_name = f"_flow_{fj.target}"
            if hasattr(wrapper, handler_name):
                return getattr(wrapper, handler_name)(fj.data, *args, **kwargs)
            else:
                # 向上传播
                raise
    return wrapper


def register_flow_target(func: Callable, target_name: str, handler: Callable):
    """
    为 flow_handler 装饰的函数注册流转目标

    编译器生成:
        claw_runtime.register_flow_target(processOrder, "approvalFlow", _approvalFlow_handler)
    """
    setattr(func, f"_flow_{target_name}", handler)


# ================================================================
#  3. 属性变更监听系统
#
#  @BeforeProps("user.age,user.name")  ->  变更前监听
#  @AfterProps("user.email,user.phone") -> 变更后监听
#
#  Python 实现：使用描述符协议 + 元类/装饰器
# ================================================================

class PropertyMonitor:
    """
    属性变更监听管理器

    全局单例，管理所有属性监听回调
    """

    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._before_callbacks: Dict[str, List[Callable]] = {}
                    cls._instance._after_callbacks: Dict[str, List[Callable]] = {}
        return cls._instance

    def register_before(self, prop_path: str, callback: Callable):
        """注册变更前回调"""
        if prop_path not in self._before_callbacks:
            self._before_callbacks[prop_path] = []
        self._before_callbacks[prop_path].append(callback)

    def register_after(self, prop_path: str, callback: Callable):
        """注册变更后回调"""
        if prop_path not in self._after_callbacks:
            self._after_callbacks[prop_path] = []
        self._after_callbacks[prop_path].append(callback)

    def notify_before(self, obj: Any, prop_path: str, new_value: Any):
        """触发变更前回调"""
        callbacks = self._before_callbacks.get(prop_path, [])
        for cb in callbacks:
            cb(obj, prop_path, new_value)

    def notify_after(self, obj: Any, prop_path: str, old_value: Any, new_value: Any):
        """触发变更后回调"""
        callbacks = self._after_callbacks.get(prop_path, [])
        for cb in callbacks:
            cb(obj, prop_path, old_value, new_value)

    def clear(self):
        """清除所有监听（测试用）"""
        self._before_callbacks.clear()
        self._after_callbacks.clear()


# 全局属性监听器实例
_prop_monitor = PropertyMonitor()


def before_prop_change(obj: Any, prop_path: str, new_value: Any):
    """
    属性变更前通知

    编译器生成:
        claw_runtime.before_prop_change(self, "list.count", new_count)
        self.count = new_count
    """
    _prop_monitor.notify_before(obj, prop_path, new_value)


def after_prop_change(obj: Any, prop_path: str, old_value: Any, new_value: Any):
    """
    属性变更后通知

    编译器生成:
        old_count = self.count
        self.count = new_count
        claw_runtime.after_prop_change(self, "list.count", old_count, self.count)
    """
    _prop_monitor.notify_after(obj, prop_path, old_value, new_value)


def register_before_props(prop_path: str, callback: Callable):
    """注册变更前监听"""
    _prop_monitor.register_before(prop_path, callback)


def register_after_props(prop_path: str, callback: Callable):
    """注册变更后监听"""
    _prop_monitor.register_after(prop_path, callback)


class ObservableProperty:
    """
    可观察属性描述符

    编译器生成 type 定义时，将带有 @BeforeProps/@AfterProps 的属性
    替换为此描述符

    示例:
        class UserList:
            count = claw_runtime.ObservableProperty("count", 0)
    """

    def __init__(self, name: str, default=None, prop_path: str = None):
        self.name = name
        self.default = default
        self.prop_path = prop_path or name
        self._storage_name = f"_claw_prop_{name}"

    def __set_name__(self, owner, name):
        self.name = name
        self._storage_name = f"_claw_prop_{name}"

    def __get__(self, obj, objtype=None):
        if obj is None:
            return self
        return getattr(obj, self._storage_name, self.default)

    def __set__(self, obj, value):
        old_value = getattr(obj, self._storage_name, self.default)

        # Before 通知
        _prop_monitor.notify_before(obj, self.prop_path, value)

        # 实际赋值
        setattr(obj, self._storage_name, value)

        # After 通知
        _prop_monitor.notify_after(obj, self.prop_path, old_value, value)


# ================================================================
#  4. 构造/析构钩子管理
#
#  @BeforeName("initialize", "this") -> 构造时调用
#  @AfterName("cleanup", "this")     -> 析构时调用
#
#  Python 实现：装饰类，注入 __init__ 和 __del__
# ================================================================

class LifecycleManager:
    """
    生命周期管理器

    管理对象的构造/析构钩子
    """

    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    # class_name -> (before_hooks, after_hooks)
                    cls._instance._hooks: Dict[str, Tuple[List[Callable], List[Callable]]] = {}
        return cls._instance

    def register_before_name(self, class_name: str, hook: Callable):
        """注册构造钩子"""
        if class_name not in self._hooks:
            self._hooks[class_name] = ([], [])
        self._hooks[class_name][0].append(hook)

    def register_after_name(self, class_name: str, hook: Callable):
        """注册析构钩子"""
        if class_name not in self._hooks:
            self._hooks[class_name] = ([], [])
        self._hooks[class_name][1].append(hook)

    def get_before_hooks(self, class_name: str) -> List[Callable]:
        if class_name in self._hooks:
            return self._hooks[class_name][0]
        return []

    def get_after_hooks(self, class_name: str) -> List[Callable]:
        if class_name in self._hooks:
            return self._hooks[class_name][1]
        return []


_lifecycle_manager = LifecycleManager()


def managed_class(cls):
    """
    装饰器：为类注入构造/析构钩子

    编译器生成:

        @claw_runtime.managed_class
        class UserInfo:
            ...

    自动调用已注册的 @BeforeName 和 @AfterName 钩子
    """
    class_name = cls.__name__
    original_init = cls.__init__ if hasattr(cls, '__init__') else None

    def new_init(self, *args, **kwargs):
        # 调用 @BeforeName 钩子
        for hook in _lifecycle_manager.get_before_hooks(class_name):
            hook(self)

        # 调用原始 __init__
        if original_init and original_init is not object.__init__:
            original_init(self, *args, **kwargs)

    def new_del(self):
        # 调用 @AfterName 钩子
        for hook in _lifecycle_manager.get_after_hooks(class_name):
            try:
                hook(self)
            except Exception:
                pass  # 析构中不抛异常

    cls.__init__ = new_init
    cls.__del__ = new_del
    return cls


def register_before_name(class_name: str, hook: Callable):
    """注册构造钩子（编译器调用）"""
    _lifecycle_manager.register_before_name(class_name, hook)


def register_after_name(class_name: str, hook: Callable):
    """注册析构钩子（编译器调用）"""
    _lifecycle_manager.register_after_name(class_name, hook)


# ================================================================
#  5. 类型辅助
#
#  Claw 类型 -> Python 类型映射辅助
# ================================================================

# Claw 类型名 -> Python 类型
TYPE_MAP: Dict[str, type] = {
    "Int":    int,
    "Float":  float,
    "String": str,
    "Bool":   bool,
    "Void":   type(None),
    "Any":    object,
}


def claw_type_check(value: Any, expected_type: str) -> bool:
    """
    Claw 类型检查

    编译器在需要类型验证时生成:
        if not claw_runtime.claw_type_check(x, "Int"):
            raise claw_runtime.ClawTypeError("expected Int")
    """
    if expected_type == "Any":
        return True
    if expected_type == "Void":
        return value is None

    py_type = TYPE_MAP.get(expected_type)
    if py_type is not None:
        return isinstance(value, py_type)

    # 自定义类型：按类名匹配
    return type(value).__name__ == expected_type


def claw_cast(value: Any, target_type: str) -> Any:
    """
    Claw 类型转换

    编译器生成:
        result = claw_runtime.claw_cast(input_val, "Int")
    """
    try:
        if target_type == "Int":
            return int(value)
        elif target_type == "Float":
            return float(value)
        elif target_type == "String":
            return str(value)
        elif target_type == "Bool":
            return bool(value)
        elif target_type == "Any":
            return value
        else:
            raise ClawTypeError(f"Cannot cast to {target_type}")
    except (ValueError, TypeError) as e:
        raise ClawTypeError(f"Cast failed: {value} -> {target_type}: {e}")


# ================================================================
#  6. 字符串辅助
# ================================================================

def claw_concat(*parts) -> str:
    """
    Claw 字符串拼接

    Claw 代码:  "Hello " + name + " age: " + age
    生成 Python: claw_runtime.claw_concat("Hello ", name, " age: ", age)

    自动将非字符串参数转为字符串
    """
    return "".join(str(p) for p in parts)


def claw_to_string(value: Any) -> str:
    """任意值转字符串"""
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "true" if value else "false"
    return str(value)


# ================================================================
#  7. 安全辅助函数
# ================================================================

def claw_safe_access(obj: Any, *attrs: str) -> Any:
    """
    安全属性访问链

    Claw 代码:  user.profile.name
    生成 Python: claw_runtime.claw_safe_access(user, "profile", "name")

    任何环节为 None 则抛出 NullError
    """
    current = obj
    path_parts = []
    for attr in attrs:
        path_parts.append(attr)
        if current is None:
            path = ".".join(path_parts[:-1])
            raise NullError(f"Null reference: {path} is null, cannot access .{attr}")
        try:
            current = getattr(current, attr)
        except AttributeError:
            path = ".".join(path_parts)
            raise NullError(f"Attribute not found: {path}")
    return current


def claw_safe_index(collection: Any, index: int) -> Any:
    """
    安全索引访问

    越界抛出 ClawIndexError 而非 Python 内置 IndexError
    """
    if collection is None:
        raise NullError("Cannot index null collection")
    try:
        return collection[index]
    except IndexError:
        raise ClawIndexError(
            f"Index {index} out of range [0, {len(collection)})"
        )


# ================================================================
#  8. 调试/日志辅助
# ================================================================

_debug_enabled = False


def enable_debug():
    global _debug_enabled
    _debug_enabled = True


def disable_debug():
    global _debug_enabled
    _debug_enabled = False


def claw_debug(message: str, *args):
    """调试输出（仅 debug 模式）"""
    if _debug_enabled:
        if args:
            print(f"[Claw Debug] {message}", *args)
        else:
            print(f"[Claw Debug] {message}")


# ================================================================
#  9. 模块初始化
# ================================================================

def _init_runtime():
    """运行时初始化（import 时自动调用）"""
    pass

_init_runtime()
