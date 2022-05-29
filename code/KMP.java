import java.util.*;
import java.io.*;

public class KMP {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Please call this program with " +
                    "two arguments which is the input file name " +
                    "and the string to search.");
        } else {
            try {
                Scanner s = new Scanner(new File(args[0]));

                // Read the entire file into one String.
                StringBuilder fileText = new StringBuilder();
                while (s.hasNextLine()) {
                    fileText.append(s.nextLine() + "\n");
                }

                System.out.println(search(fileText.toString(), args[1]));
            } catch (FileNotFoundException e) {
                System.out.println("Unable to find file called " + args[0]);
            }
        }
    }

    /**
     * Perform KMP substring search on the given text with the given pattern.
     * 
     * This should return the starting index of the first substring match if it
     * exists, or -1 if it doesn't.
     */
    public static int search(String text, String pattern) {
        int patLen = pattern.length();
        int textLen = text.length();

        int lps[] = new int[patLen]; // lps[i] is the longest prefix suffix
        int k = 0; // index in lps[]

        computeLPSArray(pattern, patLen, lps); // compute lps[] array

        int i = 0; // index for the text

        while (i < textLen) { // try matching pattern with current text

            if (pattern.charAt(k) == text.charAt(i)) { // if first character matches
                k++;
                i++;
            }

            if (k == patLen) { // if pattern is found
                int toReturn = (i - k);
                k = lps[k - 1];
                return toReturn;

            } else if (i < textLen && pattern.charAt(k) != text.charAt(i)) { // if no match

                if (k != 0) {
                    k = lps[k - 1];
                } else {
                    i = i + 1;
                }

            }

        }

        return -1;
    }

    static void computeLPSArray(String pat, int M, int lps[]) { // lps[0..M-1]
        // length of the previous longest prefix suffix
        int len = 0;
        int i = 1;
        lps[0] = 0;


        while (i < M) {
            if (pat.charAt(i) == pat.charAt(len)) { // if the current character
                len++;
                lps[i] = len;
                i++;
            } else // (pat[i] != pat[len])
            {
                if (len != 0) {
                    len = lps[len - 1];
                } else // if (len == 0)
                {
                    lps[i] = len;
                    i++;
                }
            }
        }
    }

}