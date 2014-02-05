package org.jboss.forge.kie.completers;

import org.jboss.forge.shell.completer.SimpleTokenCompleter;

import java.util.Arrays;

public class KSessionCommandCompleter extends SimpleTokenCompleter
{
    @Override
    public Iterable<?> getCompletionTokens()
    {
        return Arrays.asList("stateful", "stateless");
    }
}
