package ru.v6.mark.prototype.support;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.Assert;

import java.util.List;
import java.util.ListIterator;

public class CompositeMessageParameter implements MessageSourceResolvable {
    private static final String DEFAULT_CONNECT_MESSAGE = "composite.message.part";
    //
    private DefaultMessageSourceResolvable delegate;

    private CompositeMessageParameter(DefaultMessageSourceResolvable delegate) {
        this.delegate = delegate;
    }

    public static CompositeMessageParameter generateWithMessages(List<String> messages) {
        return generateWithMessages(DEFAULT_CONNECT_MESSAGE, messages);
    }

    public static CompositeMessageParameter generateWithMessages(String connectMessage, List<String> messages) {
        Assert.notEmpty(messages);
        //
        DefaultMessageSourceResolvable current = null;
        ListIterator<String> iterator = messages.listIterator(messages.size());
        while (iterator.hasPrevious()) {
            String message = iterator.previous();
            //
            Object[] params = new Object[]{message, (current == null ? "" : current)};
            current = new DefaultMessageSourceResolvable(new String[]{connectMessage}, params);
        }
        //
        return new CompositeMessageParameter(current);
    }

/*
    public static CompositeMessageParameter generateWithKeys(List<Pair<String, Object[]>> messages) {
        return generateWithKeys(DEFAULT_CONNECT_MESSAGE, messages);
    }

    public static CompositeMessageParameter generateWithKeys(String connectMessage, List<Pair<String, Object[]>> messages) {
        Assert.notEmpty(messages);
        //
        DefaultMessageSourceResolvable current = null;
        ListIterator<Pair<String, Object[]>> iterator = messages.listIterator(messages.size());
        while (iterator.hasPrevious()) {
            Pair<String, Object[]> message = iterator.previous();
            DefaultMessageSourceResolvable messageResolvable = new DefaultMessageSourceResolvable(new String[]{message.getValue1()}, message.getValue2());
            //
            Object[] params = new Object[]{messageResolvable, (current == null ? "" : current)};
            current = new DefaultMessageSourceResolvable(new String[]{connectMessage}, params);
        }
        //
        return new CompositeMessageParameter(current);
    }
*/

    @Override
    public String[] getCodes() {
        return delegate.getCodes();
    }

    @Override
    public Object[] getArguments() {
        return delegate.getArguments();
    }

    @Override
    public String getDefaultMessage() {
        return delegate.getDefaultMessage();
    }
}
