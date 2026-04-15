package app.simplereader;

/**
 *
 * @author david
 */
public class Sorter {
    public static int compare(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int i = 0, j = 0;
        while (i < s1.length() && j < s2.length()) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(j);

            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                StringBuilder num1 = new StringBuilder();
                StringBuilder num2 = new StringBuilder();

                while (i < s1.length() && Character.isDigit(s1.charAt(i))) {
                    num1.append(s1.charAt(i++));
                }
                while (j < s2.length() && Character.isDigit(s2.charAt(j))) {
                    num2.append(s2.charAt(j++));
                }

                long v1 = Long.parseLong(num1.toString());
                long v2 = Long.parseLong(num2.toString());

                if (v1 != v2) return Long.compare(v1, v2);
            } else {
                if (c1 != c2) return c1 - c2;
                i++;
                j++;
            }
        }
        return s1.length() - s2.length();
    }

}
