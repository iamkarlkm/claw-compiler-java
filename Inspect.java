import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Inspect {
    public static void main(String[] args) throws Exception {
        Class<?> cls = Class.forName("org.eclipse.lsp4j.services.LanguageServer", true, Thread.currentThread().getContextClassLoader());
        System.out.println("LanguageServer");
        for (Method m : cls.getMethods()) {
            if (m.getDeclaringClass().getName().equals("org.eclipse.lsp4j.services.LanguageServer")) {
                System.out.println(m);
            }
        }
        System.out.println("TextDocumentService");
        cls = Class.forName("org.eclipse.lsp4j.services.TextDocumentService", true, Thread.currentThread().getContextClassLoader());
        for (Method m : cls.getMethods()) {
            if (m.getDeclaringClass().getName().equals("org.eclipse.lsp4j.services.TextDocumentService")) {
                System.out.println(m);
            }
        }
        System.out.println("WorkspaceService");
        cls = Class.forName("org.eclipse.lsp4j.services.WorkspaceService", true, Thread.currentThread().getContextClassLoader());
        for (Method m : cls.getMethods()) {
            if (m.getDeclaringClass().getName().equals("org.eclipse.lsp4j.services.WorkspaceService")) {
                System.out.println(m);
            }
        }
    }
