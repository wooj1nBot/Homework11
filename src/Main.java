import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

public class Main {

    static Monitor monitor;

    public static void main(String[] args) {
        InputStream inputStream = System.in;
        BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String[] input = bf.readLine().split(" ");
            int step = Integer.parseInt(input[0]);
            int seed = Integer.parseInt(input[1]);
            int capacity = Integer.parseInt(input[2]);

            Random rng = new Random();
            rng.setSeed(seed);

            if(step == 1){
                System.out.print("Step-1:\n");
                System.out.printf(" Input Stream:%d %d %d\n", step, seed, capacity);
                System.out.printf(" Input Parameters:step(%d),seed(%d),capacity(%d)", step, seed, capacity);
            }

            if(step == 2){
                System.out.print("Step-2:\n");
                System.out.print(" List:");
                while (true){
                    char c = makeAlphabet(rng);
                    System.out.print(c);
                    if(c == 'Z') break;
                }
            }



            if(step == 3){
                System.out.print("Step-3:\n");
                //Producer 객체 생성 후 쓰레드 시작.
                Producer producer = new Producer(rng);
                // 전역 변수 monitor에 Monitor 객체를 생성하여 대입.
                // Consumer를 생성할 필요가 없으므로 null로 설정.
                monitor = new Monitor(null, producer, capacity);
                producer.start();
            }

            if(step == 4){
                System.out.print("Step-4:\n");
                //Producer Consumer 객체 생성 후 쓰레드 시작.
                Producer producer = new Producer(rng);
                Consumer consumer = new Consumer();
                // 전역 변수 monitor에 Monitor 객체를 생성하여 대입.
                monitor = new Monitor(consumer, producer, capacity);

                producer.start();
                consumer.start();
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // random 객체를 넘겨받아 A + (0~25) 의 랜덤한 알파벳을 리턴한다.
    static char makeAlphabet(Random random){
        int randomNumber = random.nextInt(26);
        return (char)(randomNumber + 'A');
    }

    //영문을 만드는 쓰레드
    static class Producer extends Thread{

        Random random;
        boolean isStop = false; // isStop = true 면 run()의 무한 반복이 정지한다.

        char[] chars = {'A', 'E', 'I', 'O', 'U'}; //모음을 확인하기 위한 모음 배열

        Producer(Random random){ //생성자에서 시드가 설정된 랜덤 객체를 받는다.
            this.random = random;
        }

        @Override
        public void run() {

            while (!isStop){ // isStop = true 면 run()의 무한 반복이 정지한다.
                char c = makeAlphabet(random); //랜덤 알파벳 생성
                for(char p : chars){ //for문으로 c in [모음 배열]인지 확인
                    if(p == c){
                        monitor.produce += c; //monitor의 모음 결과값에 c를 이어붙인다.
                        break;
                    }
                }
                monitor.add(c); //monitor의 add함수 호출
            }

            System.out.print(" Produce:"); // 모음 결과값 출력
            System.out.print(monitor.produce);
            if(monitor.consumer != null) {
                // 전체 결과값 출력 단, 스텝 4일때만 (monitor.consumer != null 임.)
                System.out.print("\n Consume:");
                System.out.print(monitor.consume);
            }
        }
    }

    static class Consumer extends Thread{
        boolean isStop = false;

        @Override
        public void run() {
            while (!isStop){ // isStop = true 면 run()의 무한 반복이 정지한다.
                //monitor의 consume함수 호출
                monitor.consume();
            }
        }

    }

    static class Monitor{

        int capacity; // 넘겨받은 최대 용량
        int current; // 현재 용량

        Consumer consumer; // Consumer 객체
        Producer producer; // Producer 객체

        String produce = ""; // 모음 결과값
        String consume = ""; // 전체 결과값

        //생성자로 Consumer 객체 Producer 객체 int capacity(최대 용량) 을 받는다.
        Monitor(Consumer consumer, Producer producer, int capacity){
            this.consumer = consumer;
            this.producer = producer;
            this.capacity = capacity;
        }

        //Producer가 영문을 생성할때 마다 호출 되는 함수
        public synchronized void add(char alpha){
             current++; //현재 용량 + 1
             consume += alpha; //전체 결과값에 인자인 alpha를 이어 붙인다.
             if (current == capacity) { // 현재 용량 == 최대 용량일때 더 이상 생성하지 않아야 하므로 쓰레드 정지
                 if(consumer != null) { // 스텝 4일 때 (consumer가 빈 값이 아님.)
                     try {
                         wait(); // 쓰레드 일시 정지
                     } catch (InterruptedException e) {
                         throw new RuntimeException(e);
                     }
                 }else { // 스텝 3일때 => 최대용량에 도달한 경우 쓰레드 정지이므로
                     // producer의 isStop 변수를 true로 하여 무한반복 정지.
                     producer.isStop = true;
                 }
             }else { // 쓰레드 재개
                 notify();
             }
            // 스텝 3 , 4 공통 => 생성한 문자가 Z일때 모두 정지.
             if(alpha == 'Z'){
                 producer.isStop = true;
                 if(consumer != null) {
                     consumer.isStop = true;
                 }
             }
        }

        //Consumer가 영문을 소비할때 마다 호출 되는 함수
        public synchronized void consume(){
            current--; // 현재 용량 - 1
            if(current == 0) { // 현재 용량이 0이라면 더 이상 소비를 못하므로 Consumer 정지
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }else {
                notify();
            }
        }
    }
}