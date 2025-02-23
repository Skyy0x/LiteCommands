package dev.rollczi.litecommands.scheme;

import dev.rollczi.litecommands.argument.AnnotatedParameter;
import dev.rollczi.litecommands.command.FindResult;
import dev.rollczi.litecommands.command.Invocation;
import dev.rollczi.litecommands.command.execute.ArgumentExecutor;
import dev.rollczi.litecommands.command.section.CommandSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class SimpleSchemeGenerator implements SchemeGenerator {

    @Override
    public Scheme generateScheme(FindResult<?> result, SchemeFormat schemeFormat) {
        return new Scheme(this.generate(result, schemeFormat));
    }

    @Override
    public List<String> generate(FindResult<?> result, SchemeFormat schemeFormat) {
        Invocation<?> invocation = result.getInvocation();
        List<? extends CommandSection<?>> sections = result.getSections();

        if (sections.isEmpty()) {
            return Collections.emptyList();
        }

        Iterator<String> iterator = Arrays.stream(invocation.arguments()).iterator();

        CommandSection<?> command = sections.get(0);
        Optional<? extends ArgumentExecutor<?>> executor = result.getExecutor();
        CommandSection<?> lastSection = sections.get(sections.size() - 1);
        List<CommandSection<?>> subcommand = sections.stream()
                .skip(1)
                .collect(Collectors.toList());

        StringBuilder known = new StringBuilder();

        known.append(schemeFormat.slash());
        known.append(schemeFormat.command(command));

        if (!iterator.hasNext()) {
            return this.unknown(known.toString(), lastSection, schemeFormat);
        }

        if (executor.isPresent()) {
            for (CommandSection<?> commandSection : subcommand) {
                if (!iterator.hasNext()) {
                    return this.unknown(known.toString(), commandSection, schemeFormat);
                }

                iterator.next();
                known.append(" ");
                known.append(schemeFormat.subcommand(commandSection));
            }

            for (AnnotatedParameter<?, ?> argument : result.getAllArguments()) {
                known.append(" ");
                known.append(schemeFormat.argument(argument));
            }
        }
        else {
            known.append(" ");
            known.append(schemeFormat.subcommands(lastSection.childrenSection()));
        }

        return Collections.singletonList(known.toString());
    }

    private List<String> unknown(String text, CommandSection<?> section, SchemeFormat schemeFormat) {
        List<String> schemes = new ArrayList<>();

        for (CommandSection<?> child : section.childrenSection()) {
            schemes.addAll(this.unknown(text + " " + child.getName(), child, schemeFormat));
        }

        for (ArgumentExecutor<?> executor : section.executors()) {
            schemes.add(text + " " + executor(executor, schemeFormat));
        }

        return schemes;
    }

    private String executor(ArgumentExecutor<?> executor, SchemeFormat schemeFormat) {
        return executor.annotatedParameters().stream().map(schemeFormat::argument).collect(Collectors.joining(" "));
    }

}
