import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int N = in.nextInt();
        int K = in.nextInt();
        String S = in.next();
        int s = 0;
        for (int i = S.length()-1; i >=0 ; i--) {
            s += (S.charAt(i) - '0') << (S.length()-i-1);
        }
        int t = 0;
        while (s != 0) {
            t = t ^ s;
            s >>= 1;
        }
        t >>= K - 1;
        System.out.println(t);
    }
}