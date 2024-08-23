import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class Project2 {
    static Semaphore frontDeskSlot = new Semaphore(2, true);
    static Semaphore bellhopSlot = new Semaphore(2, true);
    static Semaphore guestReady = new Semaphore(0, true);
    static Semaphore finished = new Semaphore(0,true);
    static Semaphore dropBag = new Semaphore(0, true);
    static Semaphore deliBag = new Semaphore(0, true);
    static Semaphore takeBag = new Semaphore(0, true);
    static Semaphore enterRoom = new Semaphore(0, true);
    static Semaphore tip = new Semaphore(0, true);
    static Semaphore mutex1 = new Semaphore(1, true);
    static Semaphore mutex2 = new Semaphore(1, true);
    static Semaphore mutex3 = new Semaphore(1, true);
    static Semaphore mutex4 = new Semaphore(1, true);
    static Queue<Integer> guestQueue1 = new LinkedList<>();
    static Queue<Integer> employeeQueue = new LinkedList<>();
    static Queue<Integer> roomQueue = new LinkedList<>();
    static Queue<Integer> guestQueue2 = new LinkedList<>();
    static Queue<Integer> bellhopQueue = new LinkedList<>();

    static class Guest implements Runnable{
        private final int id;
        private final int bag;

        private int empId;
        private int keyNum;
        private int bellId;
        private final Thread guessThread;

        public Guest(int id){
            this.id = id;
            this.bag = (int) (Math.random() * 6);
            guessThread = new Thread(this);
        }

        public void start(){
            guessThread.start();
            System.out.println("Guest " + id + " created");
        }

        @Override
        public void run() {
            try{
                enterHotel();
                frontDeskSlot.acquire();
                mutex1.acquire();
                guestQueue1.add(id);
                guestReady.release();
                mutex1.release();
                finished.acquire();
                mutex2.acquire();
                empId = employeeQueue.remove();
                keyNum = roomQueue.remove();
                receiveRoom();
                mutex2.release();
                if(bag > 2){
                    requestHelp();
                    bellhopSlot.acquire();
                    mutex3.acquire();
                    guestQueue2.add(id);
                    dropBag.release();
                    mutex3.release();
                    takeBag.acquire();
                    mutex4.acquire();
                    bellId = bellhopQueue.remove();
                    entersRoom();
                    mutex4.release();
                    enterRoom.release();
                    deliBag.acquire();
                    receivesBags();
                    tip.release();
                }
                else {
                    entersRoom();
                }
                retires();
            } catch (InterruptedException e) {
                guessThread.interrupt();
            }
        }

        void enterHotel(){
            switch (bag){
                case 0:
                    System.out.println("Guest " + id + " enters hotel");
                    break;
                case 1:
                    System.out.println("Guest " + id + " enters hotel with 1 bag");
                    break;
                default:
                    System.out.println("Guest " + id + " enters hotel with " + bag + " bags");
                    break;
            }
        }

        void receiveRoom(){
            System.out.println("Guest " + id + " receives room key for room " + keyNum + " from front desk employee " + empId);
        }

        void requestHelp(){
            System.out.println("Guest " + id + " requests help with bags");
        }

        void entersRoom(){
            System.out.println("Guest " + id + " enters room " + keyNum);
        }

        void receivesBags(){
            System.out.println("Guest " + id + " receives bags from bellhop " + bellId + " and gives tip");
        }
        void retires() throws InterruptedException {
            System.out.println("Guest " + id + " retires for the evening");
        }
    }

    static class FrontDesk implements Runnable{
        static int roomNum = 0;
        private final int id;
        private int fdGuess;
        private boolean stop = false;
        private final Thread frontDeskThread;

        public FrontDesk(int id){
            this.id = id;
            frontDeskThread = new Thread(this);
        }

        public void start(){
            frontDeskThread.start();
            System.out.println("Front desk employee " + id + " created");
        }

        public void stop(){
            stop = true;
            frontDeskThread.interrupt();
        }

        @Override
        public void run() {
            while(!stop) {
                try {
                    guestReady.acquire();
                    mutex1.acquire();
                    fdGuess = guestQueue1.remove();
                    mutex1.release();
                    mutex2.acquire();
                    employeeQueue.add(id);
                    roomNum++;
                    roomQueue.add(roomNum);
                    registerGuest();
                    mutex2.release();
                    finished.release();
                    frontDeskSlot.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        void registerGuest(){
            System.out.println("Front desk employee " + id + " registers guest " + fdGuess + " and assigns room " + roomNum);
        }
    }

    static class Bellhop implements Runnable{
        private final int id;
        private int gid;
        private boolean stop = false;
        private final Thread bellhopThread;

        Bellhop(int id){
            this.id = id;
            bellhopThread = new Thread(this);
        }

        public void start(){
            bellhopThread.start();
            System.out.println("Bellhop " + id + " created");
        }

        public void stop(){
            stop = true;
            bellhopThread.interrupt();
        }

        @Override
        public void run() {
            while(!stop) {
                try {
                    dropBag.acquire();
                    mutex3.acquire();
                    gid = guestQueue2.remove();
                    mutex3.release();
                    mutex4.acquire();
                    bellhopQueue.add(id);
                    mutex4.release();
                    getBag();
                    takeBag.release();
                    enterRoom.acquire();
                    deliBag();
                    deliBag.release();
                    tip.acquire();
                    bellhopSlot.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        void getBag(){
            System.out.println("Bellhop " + id + " receives bags from guest " + gid);
        }

        void deliBag(){
            System.out.println("Bellhop " + id + " delivers bags to guest " + gid);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int numGuests = 25;
        int numFrontDeskSlots = 2;
        int numBellhops = 2;
        FrontDesk[] frontDeskThreads = new FrontDesk[numFrontDeskSlots];
        Guest[] guestThreads = new Guest[numGuests];
        Bellhop[] bellhopThreads = new Bellhop[numBellhops];

        System.out.println("Simulation starts");

        // Create front desk employees
        for(int i = 0; i < numFrontDeskSlots; i++){
            frontDeskThreads[i] = new FrontDesk(i);
        }
        // Create bellhops
        for(int i = 0; i < numBellhops; i++){
            bellhopThreads[i] = new Bellhop(i);
        }

        // Create guests
        for(int i = 0; i < numGuests; i++){
            guestThreads[i] = new Guest(i);
        }

        // Start front desk employees
        for(int i = 0; i < numFrontDeskSlots; i++){
            frontDeskThreads[i].start();
        }

        // Start bellhops
        for(int i = 0; i < numBellhops; i++){
            bellhopThreads[i].start();
        }

        // Start guests
        for(int i = 0; i < numGuests; i++){
            guestThreads[i].start();
        }

        // Guess joined
        for(int i = 0; i < numGuests; i++){
            guestThreads[i].guessThread.join();
            System.out.println("Guest " + i + " joined");
        }
        // Front desk Employee joined
        for(int i = 0; i < numFrontDeskSlots; i++){
            frontDeskThreads[i].stop();
            frontDeskThreads[i].frontDeskThread.join();
        }
        // Bellhop joined
        for(int i = 0; i < numBellhops; i++){
            bellhopThreads[i].stop();
            bellhopThreads[i].bellhopThread.join();
        }
        // Wait for all thread to join
        System.out.println("Simulation ends");
    }
}