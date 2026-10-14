import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class AnimalThread extends Thread {
    private static final int MAX_DISTANCE = 100;
    private static final AtomicBoolean raceFinished = new AtomicBoolean(false);
    private static final AtomicReference<String> winner = new AtomicReference<>(null);

    private final String animalName;
    private volatile int distance = 0;

    public AnimalThread(String name, int priority) {
        this.animalName = name;
        setName(name);
        setPriority(priority);
    }

    @Override
    public void run() {
        while (distance < MAX_DISTANCE && !raceFinished.get()) {
            try {
                // Чем выше приоритет, тем короче пауза и больше шаг
                int sleepTime = Math.max(10, 100 - getPriority() * 10);
                Thread.sleep(sleepTime);

                int step = getPriority() + 1;
                distance = Math.min(distance + step, MAX_DISTANCE);

                System.out.printf("%s пробежал: %d метров (приоритет: %d)%n",
                        animalName, distance, getPriority());

                if (distance >= MAX_DISTANCE && raceFinished.compareAndSet(false, true)) {
                    winner.set(animalName);
                    System.out.printf("%n\uD83C\uDFC6 %s ПОБЕДИЛ! %n", animalName);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(animalName + " был прерван.");
                return;
            }
        }
    }

    public int getDistance() {
        return distance;
    }

    public String getAnimalName() {
        return animalName;
    }

    public void setPriorityDynamic(int newPriority) {
        setPriority(newPriority);
    }

    public static void resetRace() {
        raceFinished.set(false);
        winner.set(null);
    }

    public static boolean isRaceFinished() {
        return raceFinished.get();
    }

    public static String getWinner() {
        return winner.get();
    }
}

public class Main {
    public static void main(String[] args) {
        System.out.println("=== ГОНКА КРОЛИКА И ЧЕРЕПАХИ ===\n");

        AnimalThread.resetRace();

        AnimalThread rabbit = new AnimalThread("Кролик", Thread.MAX_PRIORITY);
        AnimalThread turtle = new AnimalThread("Черепаха", Thread.MIN_PRIORITY);

        System.out.println("Начальные приоритеты:");
        System.out.println("  Кролик:   " + rabbit.getPriority());
        System.out.println("  Черепаха: " + turtle.getPriority());
        System.out.println();

        rabbit.start();
        turtle.start();

        while (rabbit.isAlive() || turtle.isAlive()) {
            try {
                Thread.sleep(500);

                if (AnimalThread.isRaceFinished()) {
                    break;
                }

                adjustPriorityIfLosing(turtle, rabbit);
                adjustPriorityIfLosing(rabbit, turtle);

                System.out.printf("Текущее состояние: Кролик — %d м, Черепаха — %d м%n",
                        rabbit.getDistance(), turtle.getDistance());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        joinQuietly(rabbit);
        joinQuietly(turtle);

        System.out.println("\n=== ГОНКА ЗАВЕРШЕНА ===");
        System.out.println("Финальные результаты:");
        System.out.printf("  Кролик:   %d метров%n", rabbit.getDistance());
        System.out.printf("  Черепаха: %d метров%n", turtle.getDistance());

        String winner = AnimalThread.getWinner();
        if (winner != null) {
            System.out.println("\nПобедитель: " + winner);
        }
    }
    private static void adjustPriorityIfLosing(AnimalThread loser, AnimalThread leader) {
        if (!loser.isAlive()) {
            return;
        }
        if (loser.getDistance() < leader.getDistance()
                && loser.getPriority() < Thread.MAX_PRIORITY) {
            int newPriority = loser.getPriority() + 1;
            loser.setPriorityDynamic(newPriority);
            System.out.printf("»> %s отстаёт! Повышаем приоритет до: %d%n",
                    loser.getAnimalName(), newPriority);
        }
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}