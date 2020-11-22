package ru.v6.mark.prototype.exception;

public class ThrowableUtils {

    public static Throwable getRootThrowable(Throwable base) {
        return getCauseThrowable(base, null);
    }

    public static Throwable getCauseThrowable(Throwable base, Class causeClass) {
        if (base != null) {
            if (causeClass != null && causeClass.isInstance(base)) {
                return base;
            }
            //
            Throwable cause = base.getCause();
            if (base.equals(cause)) {
                return causeClass == null ? base : null;
            } else {
                Throwable root = getCauseThrowable(cause, causeClass);
                return root == null ? (causeClass == null ? base : null) : root;
            }
        }
        return null;
    }

    public static boolean isContainsCause(Throwable base, Class causeClass) {
        return causeClass != null && getCauseThrowable(base, causeClass) != null;
    }

/*
    public static List<Pair<String, Object[]>> extractMessages(Collection<ApplicationException> exceptions) {
        List<Pair<String, Object[]>> messages = new ArrayList<Pair<String, Object[]>>();
        for (ApplicationException exception : exceptions) {
            messages.add(new Pair<String, Object[]>(exception.getMessage(), exception.getParameters()));
        }
        return messages;
    }
*/
}
