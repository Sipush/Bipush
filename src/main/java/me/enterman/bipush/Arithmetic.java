package me.enterman.bipush;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Arithmetic {
	public static int ishr(int value1, int value2){
		return value1 >> value2;
	}

	public static int ishl(int value1, int value2){
		return value1 << value2;
	}

	public static void main(String... a) throws IOException {
		int i = Integer.parseInt(new BufferedReader(new InputStreamReader(System.in)).readLine());
		i = i;
		System.out.println("adsf".hashCode());
	}
}
