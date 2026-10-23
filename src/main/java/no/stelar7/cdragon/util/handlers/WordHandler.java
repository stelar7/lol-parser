package no.stelar7.cdragon.util.handlers;

import com.google.common.collect.Sets;

import java.util.*;
import java.util.stream.Stream;

/**
 * Word lists and combinations, used for hash guessing
 */
public final class WordHandler
{
    private WordHandler()
    {
        // Hide public constructor
    }
    
    public static final String[] CHARS   = new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
    public static final String[] DIGITS  = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    public static final String[] SYMBOLS = new String[]{"!", "\"", "#", "¤", "%", "&", "/", "(", ")", "=", "?", "@", "£", "$", "€", "{", "[", "]", "}", "\\", ",", ".", ";", ":", "-", "_"};
    public static final String[] ALL     = Stream.concat(Stream.concat(Arrays.stream(CHARS), Arrays.stream(DIGITS)), Arrays.stream(SYMBOLS)).toArray(String[]::new);
    
    public static Set<String> product(Set<String> words, int repeat)
    {
        List<Set<String>> preProduct = new ArrayList<>();
        for (int i = 0; i < repeat; i++)
        {
            preProduct.add(words);
        }
        
        Set<String> product = new HashSet<>();
        for (List<String> p : Sets.cartesianProduct(preProduct))
        {
            product.add(String.join("", p));
        }
        
        return product;
    }
    
    @SafeVarargs
    public static Set<String> product(Set<String>... words)
    {
        Set<String> product = new HashSet<>();
        for (List<String> p : Sets.cartesianProduct(words))
        {
            product.add(String.join("", p));
        }
        
        return product;
    }
    
    @SafeVarargs
    public static Set<String> product(String join, Set<String>... words)
    {
        Set<String> product = new HashSet<>();
        for (List<String> p : Sets.cartesianProduct(words))
        {
            product.add(String.join(join, p));
        }
        
        return product;
    }
    
    private static final String      english    = UtilHandler.readInternalAsString("dictionary/english.txt");
    public static final  Set<String> dictionary = new HashSet<>(Arrays.asList(english.split("\n")));
    
    public static List<List<String>> searchDictionary(String input)
    {
        List<List<String>> results = new ArrayList<>();
        search(input, dictionary, new Stack<>(), results);
        return results;
    }
    
    
    private static void search(String input, Set<String> dictionary, Stack<String> words, List<List<String>> results)
    {
        for (int i = 0; i < input.length(); i++)
        {
            String substring = input.substring(0, i + 1);
            
            if (dictionary.contains(substring))
            {
                words.push(substring);
                
                if (i == input.length() - 1)
                {
                    results.add(new ArrayList<>(words));
                } else
                {
                    search(input.substring(i + 1), dictionary, words, results);
                }
                
                words.pop();
            }
        }
    }
}
