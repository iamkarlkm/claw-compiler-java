#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <setjmp.h>
#include "claw_runtime.h"  /* 所有符号在这里定义 */


/* Runtime Helpers (省略) */

#define CLAW_EX_VALIDATIONERROR 1001
#define CLAW_EX_PAYMENTERROR    1002

/**
 * @brief 订单处理流程，展示三层操作流
 */
bool processOrder(void* order) {
    printf("开始处理订单");

    /* === Normal Flow with Exception Handling === */
    CLAW_TRY

        /* 业务逻辑... */
        /* 如果验证失败:
           __claw_throw(CLAW_EX_VALIDATIONERROR, "无效订单");
        */

    CLAW_CATCH(CLAW_EX_VALIDATIONERROR)
        ClawException* e = &__claw_current_exception;
        printf("验证失败: %s", e->message);
        return false;

    CLAW_CATCH(CLAW_EX_PAYMENTERROR)
        ClawException* e_pay = &__claw_current_exception;
        printf("支付失败: %s", e_pay->message);
        goto retryFlow;  /* flow to retryFlow — C 原生 goto！ */

    CLAW_END_TRY;

    printf("订单处理完成");
    return true;

retryFlow:  /* flow-to 目标标签 */    printf("进入重试流程");
    return false;
}

int main(int argc, char* argv[]) {
    bool result = processOrder(NULL);
    printf("处理结果: %s", result ? "成功" : "失败");
    return 0;
}
