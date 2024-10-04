package essentialsmagic.EssentialsMagic;

public class Utilities {

    public static String colorize(String text) {
        return text != null ? text.replace("&", "§") : "";
    }

}