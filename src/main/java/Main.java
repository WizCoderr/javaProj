import command.CommandProcessor;
import game.GameEngine;

public final class Main {
    public static void main(String[] args) {
        final GameEngine gameEngine = new GameEngine();
        final CommandProcessor commandProcessor = new CommandProcessor(gameEngine);
        commandProcessor.processCommands();
    }
}