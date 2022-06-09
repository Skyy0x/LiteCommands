package dev.rollczi.litecommands.implementation;

import dev.rollczi.litecommands.command.FindResult;
import dev.rollczi.litecommands.command.Invocation;
import dev.rollczi.litecommands.command.sugesstion.SuggestionStack;
import dev.rollczi.litecommands.command.section.CommandSection;
import dev.rollczi.litecommands.command.execute.ExecuteResult;
import dev.rollczi.litecommands.command.LiteInvocation;
import dev.rollczi.litecommands.platform.SuggestionListener;
import dev.rollczi.litecommands.platform.ExecuteListener;
import dev.rollczi.litecommands.platform.RegistryPlatform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestPlatform implements RegistryPlatform<TestHandle> {

    private final Map<CommandSection<TestHandle>, Command> commands = new HashMap<>();

    @Override
    public void registerListener(CommandSection<TestHandle> command, ExecuteListener<TestHandle> listener, SuggestionListener<TestHandle> suggestionListener) {
        this.commands.put(command, new Command(listener, suggestionListener));
    }

    @Override
    public void unregisterListener(CommandSection<TestHandle> command) {
        this.commands.remove(command);
    }

    @Override
    public void unregisterAll() {
        this.commands.clear();
    }

    public ExecuteResult execute(String command, String... args) {
        TestHandle handle = new TestHandle();
        LiteInvocation invocation = new LiteInvocation(new TestSender(handle), command, command, args);

        for (Map.Entry<CommandSection<TestHandle>, Command> entry : commands.entrySet()) {
            CommandSection<TestHandle> section = entry.getKey();
            Command cmd = entry.getValue();

            if (section.isSimilar(command)) {
                return cmd.getExecuteListener().execute(handle, invocation);
            }
        }

        return ExecuteResult.failure(FindResult.none(invocation.withHandle(handle)));
    }

    public FindResult<TestHandle> find(String command, String... args) {
        TestHandle handle = new TestHandle();
        for (Map.Entry<CommandSection<TestHandle>, Command> entry : commands.entrySet()) {
            CommandSection<TestHandle> section = entry.getKey();

            Invocation<TestHandle> liteInvocation = new Invocation<>(handle, new TestSender(handle), command, command, args);

            if (section.isSimilar(command)) {
                return section.find(liteInvocation.toLite(), 0, FindResult.none(liteInvocation));
            }
        }

        throw new IllegalArgumentException();
    }

    public List<String> suggestion(String command, String... args) {
        FindResult<TestHandle> findResult = this.find(command, args);
        SuggestionStack result = findResult.knownSuggestion();

        return result.multilevelSuggestions();
    }

    private static final class Command {

        private final ExecuteListener<TestHandle> executeListener;
        private final SuggestionListener<TestHandle> suggestionListener;

        public Command(ExecuteListener<TestHandle> executeListener, SuggestionListener<TestHandle> suggestionListener) {
            this.executeListener = executeListener;
            this.suggestionListener = suggestionListener;
        }

        public ExecuteListener<TestHandle> getExecuteListener() {
            return executeListener;
        }

        public SuggestionListener<TestHandle> getSuggester() {
            return suggestionListener;
        }

    }

}
