package snakes;

/**
 *
 */
public class BotLoader extends ClassLoader {

    /**
     * Fetches the class given the name of the class and the package,
     * the class would be taken from the classpath and could be dynamically
     * added after the game is compiled.
     *
     * @param classBinName The name of the Bot class to load.
     * @return An instance of the Bot class
     */
    public Class<? extends Bot> getBotClass(String classBinName) {
        try {
            // Create a new JavaClassLoader
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // Load the target class using its binary name
            Class<?> loadedMyClass = classLoader.loadClass(classBinName);
            return loadedMyClass.asSubclass(Bot.class);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
