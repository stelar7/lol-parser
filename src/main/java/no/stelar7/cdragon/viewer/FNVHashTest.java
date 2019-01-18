package no.stelar7.cdragon.viewer;

import no.stelar7.cdragon.util.handlers.*;

import java.util.Scanner;

@SuppressWarnings("InfiniteLoopStatement")
public class FNVHashTest
{
    public static void main(String[] args)
    {
        Scanner sc = new Scanner(System.in);
        HashHandler.loadAllWadHashes();
        while (true)
        {
            String input = sc.nextLine();
            System.out.println(HashHandler.getBINHash(input));
        }
    }
}
