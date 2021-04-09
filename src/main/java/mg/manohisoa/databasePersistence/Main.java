/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mg.manohisoa.databasePersistence;

import java.util.Locale;

/**
 *
 * @author manohisoa
 */
public class Main {

    public static void swap(int i, int j, StringBuilder sb) {
        char temp = sb.charAt(i);
        sb.setCharAt(i, sb.charAt(j));
        sb.setCharAt(j, temp);
    }

    public static void shifft(StringBuilder sb, String dir) {
        String sbTemp = sb.toString();
        if (dir.equals("R")) {
            sb.setCharAt(0, sbTemp.charAt(sb.length() - 1));
            for (int i = 0; i < sb.length(); i++) {
                if (i != sb.length() - 1) {
                    sb.setCharAt(i + 1, sbTemp.charAt(i));
                }
            }
        } else {
            sb.setCharAt(sb.length() - 1, sbTemp.charAt(0));
            for (int i = sb.length() - 1; i >= 0; i--) {
                if (i != 0) {
                    sb.setCharAt(i - 1, sbTemp.charAt(i));
                }
            }
        }

    }

    public static String tobinary(int num) {
        String s = "";
        while (num > 0) {
            int bit = num % 2;
            int quotient = num / 2;
            // put bit to the left of any previous bits in the bitstring ;
            s = bit + "" + s;
            num = quotient;
        }
        return s;
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.NoSuchMethodException
     */
    public static void main(String[] args) throws Exception {
        String format = "%,.2f";
        String from = String.format(Locale.FRENCH, format, 1000000.0);
//        System.out.println(from);
        String f2 = from.replaceAll(",", ".");
        String f3 = f2.replace('\u00a0', '\0');
        System.out.println(f3);
//        double d = Double.parseDouble("1 000 000.00");
//        System.out.println(d);
//        Double coefDir = (1.0 - 3.0) / (1.0 - 5.0);
//        Double coefDir2 = (4.0 - 1.0) / (-5.0 - -2.0);
//        System.out.println(coefDir2);
//        Double angle = Math.toDegrees(Math.atan(coefDir2));
//        System.out.println(angle);
//        StringBuilder sb = new StringBuilder("saluti");//tsalu
//        char un = 'f';
//        int aaa = un - 'a';
//        System.err.println(aaa);

//        int a = 0;
//        int[] tab = new int[2];
//        ++a;
//        tab[a] = 1;
//        System.out.println(tab[0]);
//        System.out.println(tab[1]);
//        System.out.println( 1);
//        java.sql.Date daty = java.sql.Date.valueOf("2020-06-09");
////        System.out.println("inter:" + daty.toString());
//        PGInterval inter = new PGInterval("00:10:00");
//        PGInterval inter2 = new PGInterval("00:30:10");
//        PGInterval inter3 = new PGInterval("-" + inter2.getHours() + ":" + inter2.getMinutes() + ":" + inter2.getSeconds() + "");
////        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
////        Date parsedDate = dateFormat.parse("2020-02-09 23:59:00");
////        Timestamp timestamp1 = new Timestamp(parsedDate.getTime());
////
////        Calendar cal = new GregorianCalendar();
////        cal.setTimeInMillis(timestamp1.getTime());
//
//        inter.add(inter3);
////        Timestamp timestamp2 = new Timestamp(cal.getTimeInMillis());
//        System.out.println("inter:" + inter3.toString());
//        Date parsedDate2 = dateFormat.parse("2020-02-09 12:04:00");
//        Timestamp timestamp2 = new Timestamp(parsedDate2.getTime());
//        long milliseconds = timestamp2.getTime() - timestamp1.getTime();
//        int seconds = (int) milliseconds / 1000;
//
//        int hours = seconds / 3600;
//        int minutes = (seconds % 3600) / 60;
//        seconds = (seconds % 3600) % 60;
//
//        System.out.println("timestamp1: " + timestamp1);
//        System.out.println("timestamp2: " + timestamp2);
//
//        System.out.println("Difference: ");
//        System.out.println(" Hours: " + hours);
//        System.out.println(" Minutes: " + minutes);
//        System.out.println(" Seconds: " + seconds);
//
//        PGInterval inter = new PGInterval("00:02:30");
//        PGInterval inter2 = new PGInterval("00:00:30");
//        System.out.println("inter:" + inter.toString());
//        System.out.println("inter2:" + inter2.toString());
//        inter.add(inter2);
//        System.out.println("inter:" + inter.toString());
//        System.out.println("inter2:" + inter2.toString());
//        Calendar cal = new GregorianCalendar();
//        cal.setTimeInMillis(times.getTime());
//
//        inter.add(cal);
//
//        Timestamp times2 = new Timestamp(cal.getTimeInMillis());
//        ArrayList<Integer> testsort = new ArrayList<>();
//        testsort.add(2);
//        testsort.add(3);
//        testsort.add(7);
//        testsort.add(1);
//
//        testsort.forEach((timestamp) -> {
//            System.out.println(timestamp);
//        });
//
//        Collections.sort(testsort,
//                new Comparator<Integer>() {
//            @Override
//            public int compare(Integer o1, Integer o2) {
//                return o2 - o1;
//            }
//        });
//        System.out.println(":***********************:");
//        testsort.forEach((timestamp) -> {
//            System.out.println(timestamp);
//        });
//        Connection con = null;
//        try {
//            con = GeneriqueDAO.getConnection("trosa_db", "postgres", "m1234", "postgres", 5432, "localhost");
//            // GeneriqueDAO.insert(new Trosa("sdq","sdfsf","USR0001","USR0002",29.0,Utilitaire.getCurrentTimeStamp(),1), con);
////            con.commit();
//            Trosa[] rep = (Trosa[]) GeneriqueDAO.find(new Trosa(null, null, null, null, null, null, -776), "", con);
//            for (Trosa test : rep) {
//                System.out.println("testid= " + test.getId());
//            }
//
//            System.out.println(GeneriqueDAO.CACHE.containsKey("trosa"));
//        } catch (Exception e) {
////            con.rollback();
//            e.printStackTrace();
//        } finally {
//            if (con != null) {
//                con.close();
//            }
//        }
    }

    public static void test(String bla) {
        bla += " ble";
    }

}
