import java.util.*;
import java.time.*;
import java.time.format.*;

/**
 * ============================================================
 *  Weekly Planner — Java Console Application
 * ============================================================
 *
 *  Course Outcome Mapping:
 *  CO1 – Algorithmic efficiency analysis (Big-O) applied to
 *        search, sort, and traversal operations on tasks.
 *  CO2 – ADTs using arrays and linked lists: DoublyLinkedList
 *        used as the task list per day.
 *  CO3 – Stack used for undo history; PriorityQueue used for
 *        priority-ordered task display.
 *  CO4 – HashMap (hash table) maps date keys → task lists;
 *        Java Collections (List, Queue, Deque, Map) used throughout.
 *  CO5 – Practical application: full weekly planner with
 *        add/delete/complete/stats/undo.
 *  CO6 – Complete, runnable program demonstrating real-world
 *        DS usage and program design in Java.
 * ============================================================
 */
public class WeeklyPlanner {

    // =========================================================
    //  Priority Enum
    //  CO1 – O(1) comparisons via enum ordinal
    // =========================================================
    enum Priority {
        LOW(1), MED(2), HIGH(3);

        final int level;
        Priority(int level) { this.level = level; }

        static Priority fromString(String s) {
            return switch (s.trim().toLowerCase()) {
                case "high", "h" -> HIGH;
                case "low",  "l" -> LOW;
                default          -> MED;
            };
        }
    }

    // =========================================================
    //  Task class – node for the DoublyLinkedList
    //  CO2 – Linked list node design; each Task is a list node
    // =========================================================
    static class Task {
        final int    id;
        String       text;
        boolean      done;
        Priority     priority;
        Task         prev, next;   // CO2 – doubly-linked pointers

        Task(int id, String text, Priority priority) {
            this.id       = id;
            this.text     = text;
            this.priority = priority;
            this.done     = false;
        }

        @Override
        public String toString() {
            String status = done ? "[✓]" : "[ ]";
            return String.format("%s #%-3d %-8s %s", status, id,
                    "[" + priority + "]", text);
        }
    }

    // =========================================================
    //  DoublyLinkedList<Task>
    //  CO2 – Implements insert, delete, search, traverse,
    //        reverse (O(n) traversal), cycle detection guard.
    //        Space: O(n); Insert head/tail: O(1); Search: O(n)
    // =========================================================
    static class TaskList implements Iterable<Task> {
        Task head, tail;
        int  size;

        /** CO2 – Insert at tail: O(1) */
        void addLast(Task t) {
            if (tail == null) { head = tail = t; t.prev = t.next = null; }
            else { tail.next = t; t.prev = tail; t.next = null; tail = t; }
            size++;
        }

        /** CO2 – Search by id: O(n) */
        Task findById(int id) {
            Task cur = head;
            while (cur != null) {
                if (cur.id == id) return cur;
                cur = cur.next;
            }
            return null;
        }

        /** CO2 – Delete by id: O(n) search + O(1) splice */
        boolean deleteById(int id) {
            Task t = findById(id);
            if (t == null) return false;
            if (t.prev != null) t.prev.next = t.next; else head = t.next;
            if (t.next != null) t.next.prev = t.prev; else tail = t.prev;
            size--;
            return true;
        }

        /**
         * CO2 – Reverse the list in O(n) by swapping prev/next pointers.
         * Demonstrates in-place linked-list reversal.
         */
        void reverse() {
            Task cur = head;
            Task tmp = null;
            while (cur != null) {
                tmp      = cur.prev;
                cur.prev = cur.next;
                cur.next = tmp;
                cur      = cur.prev;        // move to what was 'next'
            }
            if (tmp != null) { head = tmp.prev; }  // tmp is now new head wrapper
            // swap head/tail
            tmp  = head;
            head = tail;
            tail = tmp;
        }

        /**
         * CO2 – Floyd's cycle detection (O(n) time, O(1) space).
         * In a correctly maintained list this always returns false;
         * included to demonstrate the algorithm.
         */
        boolean hasCycle() {
            Task slow = head, fast = head;
            while (fast != null && fast.next != null) {
                slow = slow.next;
                fast = fast.next.next;
                if (slow == fast) return true;
            }
            return false;
        }

        /** CO1 – O(n) traversal */
        @Override
        public Iterator<Task> iterator() {
            return new Iterator<>() {
                Task cur = head;
                public boolean hasNext() { return cur != null; }
                public Task    next()    { Task t = cur; cur = cur.next; return t; }
            };
        }

        boolean isEmpty() { return size == 0; }
    }

    // =========================================================
    //  UndoAction – stored on a Stack
    //  CO3 – Stack-based undo; LIFO semantics, O(1) push/pop
    // =========================================================
    enum ActionType { ADD, DELETE, TOGGLE }

    static class UndoAction {
        ActionType type;
        String     dateKey;
        Task       task;          // snapshot
        UndoAction(ActionType type, String dateKey, Task task) {
            this.type    = type;
            this.dateKey = dateKey;
            this.task    = task;
        }
    }

    // =========================================================
    //  WeeklyPlanner state
    //  CO4 – HashMap<String, TaskList>: O(1) avg lookup by date
    //        key. Demonstrates hash-based data structure usage.
    // =========================================================
    private final Map<String, TaskList> taskMap  = new HashMap<>();  // CO4
    private final Deque<UndoAction>     undoStack = new ArrayDeque<>(); // CO3
    private int taskIdCounter = 1;
    private int weekOffset    = 0;

    // =========================================================
    //  Helpers
    // =========================================================
    private LocalDate weekStart() {
        return LocalDate.now()
                .with(DayOfWeek.SUNDAY)
                .plusWeeks(weekOffset);
    }

    /** CO4 – O(1) avg hash lookup to get or create a TaskList */
    private TaskList listFor(String key) {
        return taskMap.computeIfAbsent(key, k -> new TaskList());
    }

    private String fmt(LocalDate d) { return d.toString(); }   // yyyy-MM-dd

    // =========================================================
    //  Core operations
    // =========================================================

    /**
     * CO2, CO4, CO5 – Add task: O(1) hash lookup + O(1) linked-list insert.
     * Total: O(1) amortized.
     */
    void addTask(String dateKey, String text, Priority pri) {
        Task t = new Task(taskIdCounter++, text, pri);
        listFor(dateKey).addLast(t);

        // CO3 – push undo record onto stack O(1)
        undoStack.push(new UndoAction(ActionType.ADD, dateKey, t));
        System.out.println("  ✓ Added: " + t);
    }

    /**
     * CO2, CO3 – Delete task: O(n) search, O(1) splice; undo in O(1).
     */
    boolean deleteTask(String dateKey, int id) {
        TaskList list = taskMap.get(dateKey);
        if (list == null) return false;
        Task t = list.findById(id);          // CO2 – O(n) search
        if (t == null) return false;
        list.deleteById(id);                 // CO2 – O(1) splice after search
        undoStack.push(new UndoAction(ActionType.DELETE, dateKey, t)); // CO3
        System.out.println("  ✓ Deleted task #" + id);
        return true;
    }

    /**
     * CO2, CO3 – Toggle done status: O(n) search + O(1) toggle.
     */
    boolean toggleTask(String dateKey, int id) {
        TaskList list = taskMap.get(dateKey);
        if (list == null) return false;
        Task t = list.findById(id);
        if (t == null) return false;
        t.done = !t.done;
        undoStack.push(new UndoAction(ActionType.TOGGLE, dateKey, t)); // CO3
        System.out.println("  ✓ Marked task #" + id + " as " + (t.done ? "done" : "not done"));
        return true;
    }

    /**
     * CO3 – Undo: pops from Stack in O(1) and reverses the last action.
     */
    void undo() {
        if (undoStack.isEmpty()) { System.out.println("  Nothing to undo."); return; }
        UndoAction a = undoStack.pop();    // CO3 – O(1) stack pop
        switch (a.type) {
            case ADD    -> listFor(a.dateKey).deleteById(a.task.id);
            case DELETE -> listFor(a.dateKey).addLast(a.task);
            case TOGGLE -> { Task t = listFor(a.dateKey).findById(a.task.id);
                             if (t != null) t.done = !t.done; }
        }
        System.out.println("  ✓ Undo successful.");
    }

    // =========================================================
    //  Display
    // =========================================================

    /**
     * CO3 – Priority queue (min-heap) used to display tasks in
     *       priority order. O(n log n) overall.
     * CO1 – Compare to linear display: same O(n) traversal but
     *       O(n log n) sorting via heap vs O(n²) insertion sort.
     */
    void showDayPrioritized(String dateKey) {
        TaskList list = taskMap.get(dateKey);
        if (list == null || list.isEmpty()) {
            System.out.println("  (no tasks)"); return;
        }
        // CO3 – PriorityQueue: tasks ordered by priority desc, then id
        PriorityQueue<Task> pq = new PriorityQueue<>(
            Comparator.comparingInt((Task t) -> -t.priority.level)
                      .thenComparingInt(t -> t.id)
        );
        for (Task t : list) pq.offer(t);    // O(n log n) total insertions
        while (!pq.isEmpty()) {
            System.out.println("    " + pq.poll());  // O(log n) per poll
        }
    }

    /**
     * CO5 – Render the full weekly planner view.
     * CO1 – Overall display: O(n) where n = total tasks this week.
     */
    void renderWeek() {
        LocalDate ws = weekStart();
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.printf ("║  WEEKLY PLANNER  —  %s to %s   ║%n",
                ws, ws.plusDays(6));
        System.out.println("╚══════════════════════════════════════════════════╝");

        int total = 0, done = 0;
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE dd MMM");

        for (int i = 0; i < 7; i++) {
            LocalDate d   = ws.plusDays(i);
            String    key = fmt(d);
            boolean isToday = d.equals(LocalDate.now());
            TaskList  list  = taskMap.getOrDefault(key, new TaskList());

            total += list.size;
            for (Task t : list) if (t.done) done++;

            System.out.printf("%n  %s%s%n", d.format(dayFmt), isToday ? "  ◄ TODAY" : "");
            System.out.println("  " + "─".repeat(36));
            showDayPrioritized(key);
        }

        // CO5 – Weekly statistics
        int pct = total == 0 ? 0 : (int)(done * 100.0 / total);
        System.out.println();
        System.out.println("══════════════════════  STATS  ══════════════════════");
        System.out.printf("  Total: %-4d  Completed: %-4d  Remaining: %-4d  Progress: %d%%%n",
                total, done, total - done, pct);

        // Progress bar (CO5 – visual feedback)
        int bars = pct / 5;
        System.out.print("  [");
        for (int i = 0; i < 20; i++) System.out.print(i < bars ? "█" : "░");
        System.out.println("]");
        System.out.println("══════════════════════════════════════════════════════");
    }

    // =========================================================
    //  Sorting demo
    //  CO1 – Demonstrates O(n²) Insertion Sort vs O(n log n)
    //        Merge Sort on tasks, fulfilling CO1 requirement to
    //        implement and compare sorting algorithms.
    // =========================================================

    /** CO1 – Insertion Sort on array of tasks by priority desc: O(n²) */
    static Task[] insertionSort(Task[] arr) {
        Task[] a = arr.clone();
        for (int i = 1; i < a.length; i++) {
            Task key = a[i];
            int  j   = i - 1;
            while (j >= 0 && a[j].priority.level < key.priority.level) {
                a[j + 1] = a[j]; j--;
            }
            a[j + 1] = key;
        }
        return a;
    }

    /** CO1 – Merge Sort on array of tasks by priority desc: O(n log n) */
    static Task[] mergeSort(Task[] arr) {
        if (arr.length <= 1) return arr;
        int mid = arr.length / 2;
        Task[] left  = mergeSort(Arrays.copyOfRange(arr, 0, mid));
        Task[] right = mergeSort(Arrays.copyOfRange(arr, mid, arr.length));
        return merge(left, right);
    }

    private static Task[] merge(Task[] l, Task[] r) {
        Task[] res = new Task[l.length + r.length];
        int i = 0, j = 0, k = 0;
        while (i < l.length && j < r.length) {
            if (l[i].priority.level >= r[j].priority.level) res[k++] = l[i++];
            else res[k++] = r[j++];
        }
        while (i < l.length) res[k++] = l[i++];
        while (j < r.length) res[k++] = r[j++];
        return res;
    }

    /**
     * CO1 – Binary Search for a task by id in a sorted-by-id array: O(log n).
     * Linear search shown for comparison: O(n).
     */
    static int binarySearchById(Task[] sorted, int targetId) {
        int lo = 0, hi = sorted.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if      (sorted[mid].id == targetId) return mid;
            else if (sorted[mid].id  < targetId) lo = mid + 1;
            else                                  hi = mid - 1;
        }
        return -1;
    }

    // =========================================================
    //  Main – interactive console loop
    //  CO5, CO6 – Complete practical application
    // =========================================================
    public static void main(String[] args) {
        WeeklyPlanner planner = new WeeklyPlanner();
        Scanner sc = new Scanner(System.in);

        // Pre-load sample tasks so the planner is not empty on first run
        String today = LocalDate.now().toString();
        planner.addTask(today, "Review lecture notes", Priority.HIGH);
        planner.addTask(today, "Complete assignment 2", Priority.HIGH);
        planner.addTask(today, "Read chapter 5", Priority.MED);
        planner.addTask(today, "Exercise", Priority.LOW);

        String tomorrow = LocalDate.now().plusDays(1).toString();
        planner.addTask(tomorrow, "Submit lab report", Priority.HIGH);
        planner.addTask(tomorrow, "Team meeting prep", Priority.MED);

        System.out.println("\n  ══ Welcome to Weekly Planner (Java) ══");
        System.out.println("  Data Structures & Algorithms Edition\n");

        boolean running = true;
        while (running) {
            planner.renderWeek();
            System.out.println();
            System.out.println("  Commands:");
            System.out.println("  [a] Add task     [d] Delete task  [t] Toggle done");
            System.out.println("  [u] Undo         [n] Next week    [p] Prev week");
            System.out.println("  [s] Sort demo    [r] Reverse day  [c] Cycle check");
            System.out.println("  [q] Quit");
            System.out.print("  > ");

            String cmd = sc.nextLine().trim().toLowerCase();

            switch (cmd) {

                // ── Add ──────────────────────────────────────────────
                // CO2, CO4, CO5
                case "a" -> {
                    System.out.print("  Date (yyyy-mm-dd, blank = today): ");
                    String ds = sc.nextLine().trim();
                    if (ds.isEmpty()) ds = LocalDate.now().toString();
                    System.out.print("  Task text: ");
                    String text = sc.nextLine().trim();
                    System.out.print("  Priority (high/med/low): ");
                    Priority pri = Priority.fromString(sc.nextLine().trim());
                    planner.addTask(ds, text, pri);
                }

                // ── Delete ───────────────────────────────────────────
                // CO2, CO3
                case "d" -> {
                    System.out.print("  Date (yyyy-mm-dd, blank = today): ");
                    String ds = sc.nextLine().trim();
                    if (ds.isEmpty()) ds = LocalDate.now().toString();
                    System.out.print("  Task ID: ");
                    try {
                        int id = Integer.parseInt(sc.nextLine().trim());
                        if (!planner.deleteTask(ds, id))
                            System.out.println("  Task not found.");
                    } catch (NumberFormatException e) {
                        System.out.println("  Invalid ID.");
                    }
                }

                // ── Toggle ───────────────────────────────────────────
                // CO2, CO3
                case "t" -> {
                    System.out.print("  Date (yyyy-mm-dd, blank = today): ");
                    String ds = sc.nextLine().trim();
                    if (ds.isEmpty()) ds = LocalDate.now().toString();
                    System.out.print("  Task ID: ");
                    try {
                        int id = Integer.parseInt(sc.nextLine().trim());
                        if (!planner.toggleTask(ds, id))
                            System.out.println("  Task not found.");
                    } catch (NumberFormatException e) {
                        System.out.println("  Invalid ID.");
                    }
                }

                // ── Undo ─────────────────────────────────────────────
                // CO3 – Stack pop O(1)
                case "u" -> planner.undo();

                // ── Navigate ─────────────────────────────────────────
                case "n" -> planner.weekOffset++;
                case "p" -> planner.weekOffset--;

                // ── Sort Demo ────────────────────────────────────────
                // CO1 – Compare O(n²) vs O(n log n) sorting
                case "s" -> {
                    System.out.print("  Date (yyyy-mm-dd, blank = today): ");
                    String ds = sc.nextLine().trim();
                    if (ds.isEmpty()) ds = LocalDate.now().toString();
                    TaskList list = planner.taskMap.get(ds);
                    if (list == null || list.isEmpty()) {
                        System.out.println("  No tasks for that date."); break;
                    }
                    // Collect into array
                    Task[] arr = new Task[list.size];
                    int k = 0; for (Task t : list) arr[k++] = t;

                    System.out.println("\n  ── Insertion Sort (O(n²)) by priority ──");
                    for (Task t : insertionSort(arr)) System.out.println("    " + t);

                    System.out.println("\n  ── Merge Sort    (O(n log n)) by priority ──");
                    for (Task t : mergeSort(arr))     System.out.println("    " + t);

                    // Binary search demo (CO1)
                    System.out.print("\n  Binary search by ID (enter ID): ");
                    try {
                        int id     = Integer.parseInt(sc.nextLine().trim());
                        Task[] byId = arr.clone();
                        Arrays.sort(byId, Comparator.comparingInt(t -> t.id));
                        int idx = binarySearchById(byId, id);
                        System.out.println(idx >= 0
                            ? "  Found at sorted-index " + idx + ": " + byId[idx]
                            : "  Not found.");
                    } catch (NumberFormatException e) {
                        System.out.println("  Skipped.");
                    }
                }

                // ── Reverse Day List ─────────────────────────────────
                // CO2 – O(n) in-place doubly-linked-list reversal
                case "r" -> {
                    System.out.print("  Date (yyyy-mm-dd, blank = today): ");
                    String ds = sc.nextLine().trim();
                    if (ds.isEmpty()) ds = LocalDate.now().toString();
                    TaskList list = planner.taskMap.get(ds);
                    if (list == null || list.isEmpty()) {
                        System.out.println("  No tasks to reverse."); break;
                    }
                    list.reverse();    // CO2 – O(n)
                    System.out.println("  ✓ List reversed.");
                }

                // ── Cycle Check ──────────────────────────────────────
                // CO2 – Floyd's algorithm O(n) time, O(1) space
                case "c" -> {
                    System.out.print("  Date (yyyy-mm-dd, blank = today): ");
                    String ds = sc.nextLine().trim();
                    if (ds.isEmpty()) ds = LocalDate.now().toString();
                    TaskList list = planner.taskMap.get(ds);
                    boolean cycle = list != null && list.hasCycle();
                    System.out.println("  Cycle detected: " + cycle
                            + " (Floyd's O(n) algorithm)");
                }

                case "q" -> running = false;
                default  -> System.out.println("  Unknown command.");
            }
        }

        System.out.println("\n  Goodbye!\n");
        sc.close();
    }
}