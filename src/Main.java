import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

/**
 * Clinic Queue and Patient Management System (console, procedural Java).
 *
 * - Patient records are stored in parallel ArrayLists (same index = same patient).
 * - A Queue of patient IDs keeps the FIFO order of waiting patients.
 * - Urgency is a triage hint calculated from symptoms and age, not a diagnosis.
 * - Sorting (Bubble, Insertion, Selection) only rearranges records.
 *   The queue stores IDs, so sorting never changes the calling order.
 */
public class Main {

    // ===== 1. DISPLAY SETTINGS =====

    // Set to false if your terminal shows codes like [92m instead of colors
    static final boolean USE_COLOR = true;

    // Set to true to clear the screen before each menu (not supported by some IDE consoles)
    static final boolean CLEAR_SCREEN = false;

    static final String RESET   = USE_COLOR ? "\u001B[0m"  : "";
    static final String BOLD    = USE_COLOR ? "\u001B[1m"  : "";
    static final String GRAY    = USE_COLOR ? "\u001B[90m" : "";
    static final String RED     = USE_COLOR ? "\u001B[91m" : "";
    static final String GREEN   = USE_COLOR ? "\u001B[92m" : "";
    static final String YELLOW  = USE_COLOR ? "\u001B[93m" : "";
    static final String BLUE    = USE_COLOR ? "\u001B[94m" : "";
    static final String MAGENTA = USE_COLOR ? "\u001B[95m" : "";
    static final String CYAN    = USE_COLOR ? "\u001B[96m" : "";
    static final String WHITE   = USE_COLOR ? "\u001B[1m\u001B[97m" : "";

    static final int LINE_WIDTH = 90;

    // Table column widths
    static final int COL_ID = 6;
    static final int COL_NAME = 18;
    static final int COL_AGE = 8;
    static final int COL_SYMPTOMS = 30;
    static final int COL_STATUS = 10;
    static final int COL_PRIORITY = 8;

    // ===== 2. CONSTANTS =====

    // Menu choices
    static final int MENU_EXIT = 0;
    static final int MENU_REGISTER = 1;
    static final int MENU_VIEW_ALL = 2;
    static final int MENU_SEARCH = 3;
    static final int MENU_UPDATE = 4;
    static final int MENU_DELETE = 5;
    static final int MENU_ENQUEUE = 6;
    static final int MENU_DEQUEUE = 7;
    static final int MENU_PEEK = 9;
    static final int MENU_VIEW_QUEUE = 10;
    static final int MENU_QUEUE_EMPTY = 11;
    static final int MENU_BUBBLE_SORT = 12;
    static final int MENU_INSERTION_SORT = 13;
    static final int MENU_SELECTION_SORT = 14;
    static final int MENU_HISTORY = 15;
    static final int MENU_REPORT = 16;
    static final int MENU_COMPLETE = 8;

    // Patient status labels
    static final String STATUS_REGISTERED = "Registered";
    static final String STATUS_WAITING = "Waiting";
    static final String STATUS_CALLED = "Called";
    static final String STATUS_SERVED = "Served";

    static final int FIRST_PATIENT_ID = 1001;

    // Symptom checklist: SYMPTOM_NAMES[i] and SYMPTOM_WEIGHTS[i] describe the same symptom.
    // A higher weight means a more serious symptom.
    static final String[] SYMPTOM_NAMES = {
        "Fever", "Cough", "Colds / runny nose", "Sore throat", "Headache",
        "Stomach pain", "Vomiting", "Diarrhea", "Skin rash", "Body pain / weakness",
        "Dizziness", "Difficulty breathing", "Chest pain", "Injury / wound", "Other"
    };
    static final int[] SYMPTOM_WEIGHTS = {
        2, 1, 1, 1, 1,
        2, 2, 2, 1, 1,
        2, 8, 8, 3, 0 // Other is unscored and requires manual review
    };

    // Symptom positions used in the combination rules
    static final int SYM_FEVER = 0;
    static final int SYM_COUGH = 1;
    static final int SYM_VOMITING = 6;
    static final int SYM_DIARRHEA = 7;
    static final int SYM_RASH = 8;
    static final int SYM_BREATHING = 11;
    static final int SYM_CHEST_PAIN = 12;
    static final int SYM_OTHER = 14;

    // Urgency levels: 8 and above = HIGH, 4 to 7 = MEDIUM, below 4 = LOW
    static final int HIGH_PRIORITY_SCORE = 8;
    static final int MEDIUM_PRIORITY_SCORE = 4;

    // ===== 3. DATA STORAGE =====

    // Parallel ArrayLists: index i in every list = the same patient
    static ArrayList<Integer> patientIds = new ArrayList<>();
    static ArrayList<String> patientNames = new ArrayList<>();
    static ArrayList<Integer> patientAgeMonths = new ArrayList<>(); // months, so babies sort correctly
    static ArrayList<String> symptomTexts = new ArrayList<>();      // e.g. "Fever, Cough, Headache"
    static ArrayList<Integer> urgencyScores = new ArrayList<>();
    static ArrayList<Boolean> manualReviewFlags = new ArrayList<>(); // true when Other was selected
    static ArrayList<String> statuses = new ArrayList<>();

    // One line of text per action
    static ArrayList<String> activityLog = new ArrayList<>();

    // FIFO queue of patient IDs waiting for consultation
    static Queue<Integer> consultationQueue = new LinkedList<>();

    static int nextPatientId = FIRST_PATIENT_ID;
    static Scanner scanner = new Scanner(System.in);

    // ===== 4. MAIN PROGRAM =====

    public static void main(String[] args) {
        boolean running = true;

        while (running) {
            showMenu();
            int choice = readInt("Enter your choice: ", 0, 16);

            switch (choice) {
                case MENU_REGISTER -> registerPatient();
                case MENU_VIEW_ALL -> viewAllPatients();
                case MENU_SEARCH -> searchPatient();
                case MENU_UPDATE -> updateSymptoms();
                case MENU_DELETE -> deleteTreatedPatient();
                case MENU_ENQUEUE -> enqueuePatient();
                case MENU_DEQUEUE -> dequeuePatient();
                case MENU_COMPLETE -> completeConsultation();
                case MENU_PEEK -> peekNextPatient();
                case MENU_VIEW_QUEUE -> viewQueue();
                case MENU_QUEUE_EMPTY -> checkQueueEmpty();
                case MENU_BUBBLE_SORT -> bubbleSortByName();
                case MENU_INSERTION_SORT -> insertionSortByAge();
                case MENU_SELECTION_SORT -> selectionSortByUrgency();
                case MENU_HISTORY -> viewActivityHistory();
                case MENU_REPORT -> showReport();
                case MENU_EXIT -> {
                    System.out.println();
                    System.out.println(BOLD + CYAN
                            + "Thank you for using the Clinic Queue System. Goodbye!" + RESET);
                    running = false;
                }
            }

            if (running) {
                pauseBeforeMenu();
            }
        }
        scanner.close();
    }

    // ===== 5. SCREEN DESIGN HELPERS =====

    // Waits for Enter so the result stays on screen
    static void pauseBeforeMenu() {
        System.out.println();
        System.out.print(GRAY + "Press Enter to return to the menu..." + RESET);
        scanner.nextLine();
    }

    static void clearScreen() {
        if (CLEAR_SCREEN) {
            System.out.print("\u001B[H\u001B[2J");
            System.out.flush();
        }
    }

    static void showMenu() {
        clearScreen();
        System.out.println();
        System.out.println(divider());
        System.out.println(BOLD + CYAN + "CLINIC QUEUE SYSTEM" + RESET);
        System.out.println(divider());
        System.out.println();

        printMenuSection("PATIENTS", BLUE);
        printMenuItem(MENU_REGISTER, "Register patient", BLUE);
        printMenuItem(MENU_VIEW_ALL, "View all patients", BLUE);
        printMenuItem(MENU_SEARCH, "Search patient", BLUE);
        printMenuItem(MENU_UPDATE, "Update symptoms", BLUE);
        printMenuItem(MENU_DELETE, "Delete treated patient", BLUE);
        System.out.println();

        printMenuSection("QUEUE", YELLOW);
        printMenuItem(MENU_ENQUEUE, "Add patient to queue", YELLOW);
        printMenuItem(MENU_DEQUEUE, "Call next patient", YELLOW);
        printMenuItem(MENU_COMPLETE, "Complete consultation", YELLOW);
        printMenuItem(MENU_PEEK, "Peek next patient", YELLOW);
        printMenuItem(MENU_VIEW_QUEUE, "View queue", YELLOW);
        printMenuItem(MENU_QUEUE_EMPTY, "Check if queue is empty", YELLOW);
        System.out.println();

        printMenuSection("SORTING", MAGENTA);
        printMenuItem(MENU_BUBBLE_SORT, "Sort by name", MAGENTA);
        printMenuItem(MENU_INSERTION_SORT, "Sort by age", MAGENTA);
        printMenuItem(MENU_SELECTION_SORT, "Sort by urgency", MAGENTA);
        System.out.println();

        printMenuSection("HISTORY AND REPORTS", CYAN);
        printMenuItem(MENU_HISTORY, "Activity history", CYAN);
        printMenuItem(MENU_REPORT, "Reports and statistics", CYAN);
        System.out.println();

        printMenuItem(MENU_EXIT, "Exit", RED);
        System.out.println();
        System.out.println(divider());
        System.out.println(GRAY + " Waiting: " + RESET + BOLD + YELLOW + consultationQueue.size() + RESET
                + GRAY + "     Records: " + RESET + BOLD + CYAN + patientIds.size() + RESET);
        System.out.println();
    }

    static void printMenuSection(String name, String color) {
        System.out.println(BOLD + color + " " + name + RESET);
    }

    static void printMenuItem(int number, String label, String color) {
        String num = (number < 10 ? " " : "") + number;
        System.out.println("   " + BOLD + color + num + RESET + "   " + label);
    }

    static String divider() {
        return GRAY + "-".repeat(LINE_WIDTH) + RESET;
    }

    static void printTitle(String text) {
        System.out.println();
        System.out.println(divider());
        System.out.println(BOLD + CYAN + text + RESET);
        System.out.println(divider());
    }

    static void printSuccess(String msg) {
        System.out.println(BOLD + GREEN + "Success: " + RESET + GREEN + msg + RESET);
    }

    static void printError(String msg) {
        System.out.println(BOLD + RED + "Error: " + RESET + RED + msg + RESET);
    }

    static void printWarning(String msg) {
        System.out.println(YELLOW + msg + RESET);
    }

    static void printInfo(String msg) {
        System.out.println(GRAY + msg + RESET);
    }

    // Pads text with spaces to a fixed width (shortens it if too long)
    static String pad(String text, int width) {
        if (text.length() > width) {
            return text.substring(0, width - 1) + ".";
        }
        return text + " ".repeat(width - text.length());
    }

    // Months to readable text: "<1 mo", "X mo" (under 2 years), or "X yrs"
    static String formatAge(int months) {
        if (months == 0) {
            return "<1 mo";
        }
        if (months < 24) {
            return months + " mo";
        }
        return (months / 12) + " yrs";
    }

    static String statusColor(String status) {
        if (status.equals(STATUS_CALLED)) return MAGENTA;
        if (status.equals(STATUS_WAITING)) return YELLOW;
        if (status.equals(STATUS_SERVED)) return GREEN;
        return CYAN;
    }

    static String priorityLabel(int score) {
        if (score >= HIGH_PRIORITY_SCORE) return "HIGH";
        if (score >= MEDIUM_PRIORITY_SCORE) return "MEDIUM";
        return "LOW";
    }

    static String displayPriority(int index) {
        return manualReviewFlags.get(index) ? "REVIEW" : priorityLabel(urgencyScores.get(index));
    }

    static String displayPriorityColor(int index) {
        return manualReviewFlags.get(index) ? MAGENTA : priorityColor(urgencyScores.get(index));
    }

    static void showManualReviewNotice(int index) {
        if (manualReviewFlags.get(index)) {
            printWarning("MANUAL REVIEW REQUIRED: Unlisted symptom. Notify clinic staff for assessment.");
        }
    }

    static String priorityColor(int score) {
        if (score >= HIGH_PRIORITY_SCORE) return RED;
        if (score >= MEDIUM_PRIORITY_SCORE) return YELLOW;
        return GREEN;
    }

    // Prints all records in their current list order
    static void printRecordsTable() {
        System.out.println(BOLD + CYAN + pad("ID", COL_ID) + "  " + pad("NAME", COL_NAME) + "  "
                + pad("AGE", COL_AGE) + "  " + pad("SYMPTOMS", COL_SYMPTOMS) + "  "
                + pad("STATUS", COL_STATUS) + "  " + pad("PRIORITY", COL_PRIORITY) + RESET);
        System.out.println(GRAY + "-".repeat(COL_ID) + "  " + "-".repeat(COL_NAME) + "  "
                + "-".repeat(COL_AGE) + "  " + "-".repeat(COL_SYMPTOMS) + "  "
                + "-".repeat(COL_STATUS) + "  " + "-".repeat(COL_PRIORITY) + RESET);

        for (int i = 0; i < patientIds.size(); i++) {
            String status = statuses.get(i);
            System.out.println(GRAY + pad("" + patientIds.get(i), COL_ID) + RESET + "  "
                    + WHITE + pad(patientNames.get(i), COL_NAME) + RESET + "  "
                    + pad(formatAge(patientAgeMonths.get(i)), COL_AGE) + "  "
                    + pad(symptomTexts.get(i), COL_SYMPTOMS) + "  "
                    + statusColor(status) + pad(status, COL_STATUS) + RESET + "  "
                    + displayPriorityColor(i) + BOLD + pad(displayPriority(i), COL_PRIORITY) + RESET);
        }
    }

    // Prints one patient's full details
    static void printPatientCard(int index) {
        String status = statuses.get(index);
        System.out.println(WHITE + patientNames.get(index) + RESET
                + GRAY + "  (ID " + patientIds.get(index) + ")" + RESET);
        System.out.println(GRAY + "  Age:        " + RESET + formatAge(patientAgeMonths.get(index)));
        System.out.println(GRAY + "  Symptoms:   " + RESET + symptomTexts.get(index));
        System.out.println(GRAY + "  Priority:   " + RESET + displayPriorityColor(index) + BOLD
                + displayPriority(index) + RESET);
        showManualReviewNotice(index);
        System.out.println(GRAY + "  Status:     " + RESET + statusColor(status) + BOLD + status + RESET);
    }

    static void printBeforeSorting() {
        System.out.println(BOLD + YELLOW + "Before sorting" + RESET);
        System.out.println();
        printRecordsTable();
    }

    static void printAfterSorting(String description) {
        System.out.println();
        System.out.println(BOLD + GREEN + "After sorting (" + description + ")" + RESET);
        System.out.println();
        printRecordsTable();
        System.out.println();
        printInfo("Queue order was not changed.");
    }

    // ===== 6. PATIENT RECORD OPERATIONS =====

    static void registerPatient() {
        printTitle("Register Patient");
        String name = readNonEmptyString("Name: ");
        int ageInMonths = readAgeInMonths();

        boolean[] selected = readSymptomSelection();
        String symptomText = buildSymptomText(selected);
        int id = addPatient(name, ageInMonths, selected, symptomText);

        int index = findIndexById(id);
        logActivity("Registered: " + name + " (ID " + id + ")");
        System.out.println();
        printSuccess("Patient registered.");
        printPatientID("Patient ID: " + id);
        System.out.println(GRAY + "Symptoms:  " + RESET + symptomText);
        System.out.println(GRAY + "Priority:  " + RESET + displayPriorityColor(index) + BOLD
            + displayPriority(index) + RESET);
        showManualReviewNotice(index);
    }

    // Adds one patient to ALL lists so the indexes stay in sync. Returns the new ID.
    static int addPatient(String name, int ageInMonths, boolean[] selected, String symptomText) {
        int id = nextPatientId;
        patientIds.add(id);
        patientNames.add(name);
        patientAgeMonths.add(ageInMonths);
        symptomTexts.add(symptomText);
        urgencyScores.add(calculateUrgency(selected, ageInMonths));
        manualReviewFlags.add(selected[SYM_OTHER]);
        statuses.add(STATUS_REGISTERED);
        nextPatientId++;
        return id;
    }

    static void viewAllPatients() {
        printTitle("All Patients");
        if (patientIds.isEmpty()) {
            printWarning("No patient records found. Please register a patient first.");
            return;
        }
        printRecordsTable();
        System.out.println();
        printInfo("REVIEW means an unlisted symptom requires staff assessment.");
        printInfo("Total records: " + patientIds.size());
    }

    static void updateSymptoms() {
        printTitle("Update Symptoms");
        if (patientIds.isEmpty()) {
            printWarning("No patient records to update.");
            return;
        }
        int id = readInt("Patient ID: ", 1, Integer.MAX_VALUE);
        int index = findIndexById(id);
        if (index == -1) {
            printError("Patient ID " + id + " was not found.");
            return;
        }

        boolean[] selected = readSymptomSelection();
        symptomTexts.set(index, buildSymptomText(selected));
        urgencyScores.set(index, calculateUrgency(selected, patientAgeMonths.get(index)));
        manualReviewFlags.set(index, selected[SYM_OTHER]);
        logActivity("Updated symptoms: " + patientNames.get(index) + " (ID " + id + ")");

        System.out.println();
        printSuccess("Symptoms updated. New priority: " + displayPriority(index));
        showManualReviewNotice(index);
    }

    // Only Served patients can be deleted, so the queue never points to a missing record
    static void deleteTreatedPatient() {
        printTitle("Delete Treated Patient");
        if (patientIds.isEmpty()) {
            printWarning("No patient records to delete.");
            return;
        }
        int id = readInt("Patient ID: ", 1, Integer.MAX_VALUE);
        int index = findIndexById(id);
        if (index == -1) {
            printError("Patient ID " + id + " was not found.");
            return;
        }
        if (!statuses.get(index).equals(STATUS_SERVED)) {
            printWarning("Cannot delete " + patientNames.get(index) + ". Current status: "
                    + statuses.get(index) + ".");
            printInfo("Only patients with confirmed completed consultation (status Served) can be deleted.");
            return;
        }

        System.out.println();
        printPatientCard(index);
        System.out.println();
        System.out.println("  " + BOLD + RED + "1" + RESET + "   Yes, delete this record");
        System.out.println("  " + BOLD + GREEN + "2" + RESET + "   No, keep it");
        System.out.println();
        int confirm = readInt("Delete this record? ", 1, 2);
        if (confirm == 2) {
            printInfo("Deletion cancelled. The record was kept.");
            return;
        }

        // Remove from ALL lists so the indexes stay in sync
        String deletedName = patientNames.get(index);
        patientIds.remove(index);
        patientNames.remove(index);
        patientAgeMonths.remove(index);
        symptomTexts.remove(index);
        urgencyScores.remove(index);
        manualReviewFlags.remove(index);
        statuses.remove(index);

        logActivity("Deleted: " + deletedName + " (ID " + id + ")");
        System.out.println();
        printSuccess(deletedName + " was deleted. Records remaining: " + patientIds.size());
    }

    // Linear search: returns the list index of a patient ID, or -1 if not found
    static int findIndexById(int id) {
        for (int i = 0; i < patientIds.size(); i++) {
            if (patientIds.get(i) == id) {
                return i;
            }
        }
        return -1;
    }

    // ===== 7. SYMPTOM SELECTION AND URGENCY SCORING =====

    // Shows the checklist and lets the user pick many symptoms (example: 1,2,5).
    // Returns an array where selected[i] is true if symptom i was chosen.
    static boolean[] readSymptomSelection() {
        System.out.println();
        System.out.println(BOLD + "Select symptoms" + RESET);
        System.out.println();

        System.out.println();
        printSymptomChecklist();
        System.out.println();

        while (true) {
            System.out.print(CYAN + "> " + RESET + "Symptom numbers: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                printError("Please choose at least one symptom.");
                continue;
            }

            boolean[] selected = new boolean[SYMPTOM_NAMES.length];
            String[] parts = input.replace(",", " ").trim().split("\\s+");
            boolean valid = true;

            for (String part : parts) {
                try {
                    int number = Integer.parseInt(part);
                    if (number < 1 || number > SYMPTOM_NAMES.length) {
                        printError(number + " is not on the list. Choose from 1 to " + SYMPTOM_NAMES.length + ".");
                        valid = false;
                        break;
                    }
                    selected[number - 1] = true; // choosing the same number twice is ignored
                } catch (NumberFormatException e) {
                    printError("\"" + part + "\" is not a number. Example: 1,2,5");
                    valid = false;
                    break;
                }
            }

            if (valid) {
                return selected;
            }
        }
    }

    // Prints the checklist in two columns
    static void printSymptomChecklist() {
        int half = (SYMPTOM_NAMES.length + 1) / 2;
        for (int i = 0; i < half; i++) {
            String left = symptomChecklistItem(i);
            String right = (i + half < SYMPTOM_NAMES.length) ? symptomChecklistItem(i + half) : "";
            System.out.println("  " + left + "   " + right);
        }
    }

    static String symptomChecklistItem(int i) {
        String num = (i + 1 < 10 ? " " : "") + (i + 1);
        return BOLD + BLUE + num + RESET + "  " + pad(SYMPTOM_NAMES[i], 24);
    }

    // Builds the text saved in the record. If "Other" was chosen, the user types it.
    static String buildSymptomText(boolean[] selected) {
        String otherText = "";
        if (selected[SYM_OTHER]) {
            otherText = readNonEmptyString("Describe the other symptom: ");
        }
        return joinSymptoms(selected, otherText);
    }

    // Joins the selected symptom names into one text, e.g. "Fever, Cough, Headache"
    static String joinSymptoms(boolean[] selected, String otherText) {
        String text = "";
        for (int i = 0; i < selected.length; i++) {
            if (!selected[i]) {
                continue;
            }
            String part = (i == SYM_OTHER) ? otherText : SYMPTOM_NAMES[i];
            if (!text.isEmpty()) {
                text += ", ";
            }
            text += part;
        }
        return text;
    }

    // Urgency score = symptom weights + combination bonuses + age bonuses.
    // Other has no automatic symptom weight; its free text is NOT clinically evaluated.
    // The separate manualReviewFlags list prevents a provisional LOW score from appearing as final.
    // A triage hint for staff only, not a medical diagnosis.
    static int calculateUrgency(boolean[] selected, int ageInMonths) {
        int score = 0;

        // 1) symptom weights
        for (int i = 0; i < selected.length; i++) {
            if (selected[i] && i != SYM_OTHER) {
                score += SYMPTOM_WEIGHTS[i];
            }
        }

        // 2) risky combinations
        if (selected[SYM_VOMITING] && selected[SYM_DIARRHEA]) {
            score += 2; // risk of dehydration
        }
        if (selected[SYM_FEVER] && selected[SYM_RASH]) {
            score += 2; // fever with rash needs attention
        }
        if (selected[SYM_FEVER] && selected[SYM_COUGH]) {
            score += 1; // possible respiratory infection
        }
        if (selected[SYM_BREATHING] && selected[SYM_CHEST_PAIN]) {
            score += 2; // serious chest / breathing problem
        }

        // 3) age factors
        if (ageInMonths < 3 && selected[SYM_FEVER]) {
            score += 4; // fever in a very young infant is always urgent
        }
        if (ageInMonths < 12) {
            score += 2; // infants are higher risk
        }
        if (ageInMonths >= 65 * 12) {
            score += 1; // elderly patients are higher risk
        }

        return score;
    }

    // ===== 8. QUEUE OPERATIONS (FIFO) =====

    // Enqueue: add the patient's ID to the back of the queue
    static void enqueuePatient() {
        printTitle("Add Patient to Queue");
        if (patientIds.isEmpty()) {
            printWarning("No patients registered yet. Register a patient first.");
            return;
        }
        int id = readInt("Patient ID: ", 1, Integer.MAX_VALUE);
        int index = findIndexById(id);

        if (index == -1) {
            printError("Patient ID " + id + " was not found.");
            return;
        }
        if (statuses.get(index).equals(STATUS_WAITING)) {
            printWarning(patientNames.get(index) + " is already in the queue.");
            return;
        }
        if (statuses.get(index).equals(STATUS_SERVED) || statuses.get(index).equals(STATUS_CALLED)) {
            printWarning(patientNames.get(index) + " was already called or served.");
            return;
        }

        consultationQueue.add(id);
        statuses.set(index, STATUS_WAITING);
        logActivity("Added to queue: " + patientNames.get(index) + " (ID " + id
                + "), position " + consultationQueue.size());
        System.out.println();
        printSuccess(patientNames.get(index) + " added to the queue. Position: "
                + consultationQueue.size());

        showManualReviewNotice(index);

        // The queue stays FIFO. High priority only shows a notice to the staff.
        if (urgencyScores.get(index) >= HIGH_PRIORITY_SCORE) {
            printWarning("HIGH PRIORITY patient. Please notify the staff or doctor.");
        }
    }

    // Dequeue: remove and call the patient at the front of the queue
    static void dequeuePatient() {
        printTitle("Call Next Patient");
        if (consultationQueue.isEmpty()) {
            printWarning("The queue is empty. No patient to call.");
            return;
        }
        int id = consultationQueue.poll();
        int index = findIndexById(id);
        statuses.set(index, STATUS_CALLED);
        logActivity("Called: " + patientNames.get(index) + " (ID " + id + ")");

        System.out.println(GRAY + "Now calling:  " + RESET + BOLD + MAGENTA + patientNames.get(index) + RESET);
        System.out.println(GRAY + "Patient ID:   " + RESET + id);
        System.out.println(GRAY + "Symptoms:     " + RESET + symptomTexts.get(index));
        System.out.println(GRAY + "Priority:     " + RESET + displayPriorityColor(index) + BOLD
                + displayPriority(index) + RESET);
        showManualReviewNotice(index);
        System.out.println();
        printInfo("Still waiting: " + consultationQueue.size());
        printInfo("Status is Called, not Served, until consultation is confirmed complete.");
    }

    // Staff confirms completion separately from calling the patient.
    static void completeConsultation() {
        printTitle("Complete Consultation");
        if (patientIds.isEmpty()) {
            printWarning("No patient records to complete.");
            return;
        }
        int id = readInt("Patient ID: ", 1, Integer.MAX_VALUE);
        int index = findIndexById(id);
        if (index == -1) {
            printError("Patient ID " + id + " was not found.");
            return;
        }
        if (!statuses.get(index).equals(STATUS_CALLED)) {
            printWarning("Patient must be called before completing consultation. Current status: "
                    + statuses.get(index));
            return;
        }
        if (manualReviewFlags.get(index)) {
            printWarning("Unlisted symptoms require manual clinical assessment.");
            int reviewed = readInt("Has staff completed manual assessment? (1=Yes, 2=No): ", 1, 2);
            if (reviewed != 1) {
                printInfo("Completion cancelled. Manual assessment remains required.");
                return;
            }
        }
        int confirm = readInt("Confirm consultation completed? (1=Yes, 2=No): ", 1, 2);
        if (confirm != 1) {
            printInfo("Completion cancelled.");
            return;
        }
        statuses.set(index, STATUS_SERVED);
        logActivity("Consultation completed: " + patientNames.get(index) + " (ID " + id + ")");
        printSuccess("Consultation completed for " + patientNames.get(index) + ".");
        if (manualReviewFlags.get(index)) {
            printInfo("REVIEW remains visible because the unlisted symptom has no automatic score.");
        }
    }

    // Peek: look at the front of the queue without removing it
    static void peekNextPatient() {
        printTitle("Next Patient");
        if (consultationQueue.isEmpty()) {
            printWarning("The queue is empty. No patient is waiting.");
            return;
        }
        int id = consultationQueue.peek();
        int index = findIndexById(id);

        System.out.println(GRAY + "Next in line: " + RESET + BOLD + MAGENTA + patientNames.get(index) + RESET);
        System.out.println(GRAY + "Patient ID:   " + RESET + id);
        System.out.println(GRAY + "Age:          " + RESET + formatAge(patientAgeMonths.get(index)));
        System.out.println(GRAY + "Symptoms:     " + RESET + symptomTexts.get(index));
        System.out.println(GRAY + "Priority:     " + RESET + displayPriorityColor(index) + BOLD
                + displayPriority(index) + RESET);
        showManualReviewNotice(index);
    }

    // Display the queue from front to back
    static void viewQueue() {
        printTitle("Consultation Queue  (front to back)");
        if (consultationQueue.isEmpty()) {
            printWarning("The queue is empty.");
            return;
        }
        int position = 1;
        for (int id : consultationQueue) {
            int index = findIndexById(id);
            String marker = (position == 1) ? "   " + BOLD + GREEN + "<- next" + RESET : "";
            System.out.println(BOLD + YELLOW + pad(position + ".", 4) + RESET
                    + WHITE + pad(patientNames.get(index), 18) + RESET
                    + GRAY + pad("ID " + id, 10) + RESET
                    + pad(symptomTexts.get(index), 30) + "  "
                    + displayPriorityColor(index) + BOLD + pad(displayPriority(index), 8) + RESET + marker);
            position++;
        }
        System.out.println();
        printInfo("Total waiting: " + consultationQueue.size());
        printInfo("REVIEW means an unlisted symptom requires staff assessment.");
    }

    static void checkQueueEmpty() {
        printTitle("Queue Status");
        if (consultationQueue.isEmpty()) {
            printInfo("The queue is empty.");
        } else {
            printInfo("The queue is not empty. " + consultationQueue.size() + " patient(s) waiting.");
        }
    }

    // ===== 9. SORTING ALGORITHMS (written manually) =====

    // Bubble Sort by name (A-Z, ignoring case): swaps neighbors that are out of order
    static void bubbleSortByName() {
        printTitle("Bubble Sort - Sort by Name");
        if (patientIds.size() < 2) {
            printWarning("Need at least 2 records to sort.");
            return;
        }
        printBeforeSorting();

        int n = patientIds.size();
        for (int pass = 0; pass < n - 1; pass++) {
            boolean swapped = false;
            for (int j = 0; j < n - 1 - pass; j++) {
                if (patientNames.get(j).compareToIgnoreCase(patientNames.get(j + 1)) > 0) {
                    swapRecords(j, j + 1);
                    swapped = true;
                }
            }
            if (!swapped) {
                break; // already sorted
            }
        }

        logActivity("Sorted records by name (Bubble Sort)");
        printAfterSorting("name, A-Z");
    }

    // Insertion Sort by age in months (youngest first):
    // inserts each record into its correct place among the sorted records on its left
    static void insertionSortByAge() {
        printTitle("Insertion Sort - Sort by Age");
        if (patientIds.size() < 2) {
            printWarning("Need at least 2 records to sort.");
            return;
        }
        printBeforeSorting();

        int n = patientIds.size();
        for (int i = 1; i < n; i++) {
            // Hold the record to insert
            int keyId = patientIds.get(i);
            String keyName = patientNames.get(i);
            int keyAgeMonths = patientAgeMonths.get(i);
            String keySymptoms = symptomTexts.get(i);
            int keyUrgency = urgencyScores.get(i);
            String keyStatus = statuses.get(i);
            boolean keyManualReview = manualReviewFlags.get(i);

            // Shift older records one position to the right
            int j = i - 1;
            while (j >= 0 && patientAgeMonths.get(j) > keyAgeMonths) {
                patientIds.set(j + 1, patientIds.get(j));
                patientNames.set(j + 1, patientNames.get(j));
                patientAgeMonths.set(j + 1, patientAgeMonths.get(j));
                symptomTexts.set(j + 1, symptomTexts.get(j));
                urgencyScores.set(j + 1, urgencyScores.get(j));
                manualReviewFlags.set(j + 1, manualReviewFlags.get(j));
                statuses.set(j + 1, statuses.get(j));
                j--;
            }

            // Place the held record in its correct position
            patientIds.set(j + 1, keyId);
            patientNames.set(j + 1, keyName);
            patientAgeMonths.set(j + 1, keyAgeMonths);
            symptomTexts.set(j + 1, keySymptoms);
            urgencyScores.set(j + 1, keyUrgency);
            manualReviewFlags.set(j + 1, keyManualReview);
            statuses.set(j + 1, keyStatus);
        }

        logActivity("Sorted records by age (Insertion Sort)");
        printAfterSorting("age, youngest first");
    }

    // Selection Sort by urgency (highest first):
    // finds the most urgent record in the unsorted part and moves it to the front
    static void selectionSortByUrgency() {
        printTitle("Selection Sort - Sort by Urgency");
        if (patientIds.size() < 2) {
            printWarning("Need at least 2 records to sort.");
            return;
        }
        printBeforeSorting();

        int n = patientIds.size();
        for (int i = 0; i < n - 1; i++) {
            int highestIndex = i;
            for (int j = i + 1; j < n; j++) {
                // Fully scored patients come first; REVIEW records are grouped separately.
                boolean candidateReviewed = manualReviewFlags.get(j);
                boolean highestReviewed = manualReviewFlags.get(highestIndex);
                if ((!candidateReviewed && highestReviewed)
                        || (candidateReviewed == highestReviewed && !candidateReviewed
                            && urgencyScores.get(j) > urgencyScores.get(highestIndex))) {
                    highestIndex = j;
                }
            }
            if (highestIndex != i) {
                swapRecords(i, highestIndex);
            }
        }

        logActivity("Sorted records by urgency (Selection Sort)");
        printAfterSorting("assessed urgency highest first; REVIEW records last");
        printInfo("REVIEW patients are not ranked by provisional urgency scores.");
    }

    // Swaps two records across ALL lists so they stay in sync
    static void swapRecords(int a, int b) {
        int tempId = patientIds.get(a);
        patientIds.set(a, patientIds.get(b));
        patientIds.set(b, tempId);

        String tempName = patientNames.get(a);
        patientNames.set(a, patientNames.get(b));
        patientNames.set(b, tempName);

        int tempAgeMonths = patientAgeMonths.get(a);
        patientAgeMonths.set(a, patientAgeMonths.get(b));
        patientAgeMonths.set(b, tempAgeMonths);

        String tempSymptoms = symptomTexts.get(a);
        symptomTexts.set(a, symptomTexts.get(b));
        symptomTexts.set(b, tempSymptoms);

        int tempUrgency = urgencyScores.get(a);
        urgencyScores.set(a, urgencyScores.get(b));
        urgencyScores.set(b, tempUrgency);

        boolean tempManualReview = manualReviewFlags.get(a);
        manualReviewFlags.set(a, manualReviewFlags.get(b));
        manualReviewFlags.set(b, tempManualReview);

        String tempStatus = statuses.get(a);
        statuses.set(a, statuses.get(b));
        statuses.set(b, tempStatus);
    }

    // ===== 10. SEARCH OPERATIONS =====

    static void searchPatient() {
        printTitle("Search Patient");
        if (patientIds.isEmpty()) {
            printWarning("No patient records to search.");
            return;
        }
        System.out.println("  " + BOLD + BLUE + "1" + RESET + "   Search by ID");
        System.out.println("  " + BOLD + BLUE + "2" + RESET + "   Search by name");
        System.out.println();
        int option = readInt("Choose search type: ", 1, 2);

        if (option == 1) {
            searchById();
        } else {
            searchByName();
        }
    }

    static void searchById() {
        int id = readInt("Patient ID: ", 1, Integer.MAX_VALUE);
        int index = findIndexById(id);
        System.out.println();
        if (index == -1) {
            printError("No patient found with ID " + id + ".");
        } else {
            printPatientCard(index);
        }
    }

    static void searchByName() {
        String keyword = readNonEmptyString("Name (or part of it): ").toLowerCase();
        System.out.println();
        boolean found = false;
        for (int i = 0; i < patientNames.size(); i++) {
            if (patientNames.get(i).toLowerCase().contains(keyword)) {
                printPatientCard(i);
                found = true;
            }
        }
        if (!found) {
            printError("No patient found matching \"" + keyword + "\".");
        }
    }

    // ===== 11. ACTIVITY HISTORY AND REPORTS =====

    static void logActivity(String message) {
        activityLog.add(message);
    }

    static void viewActivityHistory() {
        printTitle("Activity History  (oldest to newest)");
        if (activityLog.isEmpty()) {
            printWarning("No activity yet. Actions will be recorded here.");
            return;
        }
        for (int i = 0; i < activityLog.size(); i++) {
            String entry = activityLog.get(i);
            System.out.println(GRAY + pad((i + 1) + ".", 5) + RESET
                    + activityColor(entry) + entry + RESET);
        }
        System.out.println();
        printInfo("Total actions recorded: " + activityLog.size());
    }

    static String activityColor(String entry) {
        if (entry.startsWith("Called")) return MAGENTA;
        if (entry.startsWith("Added to queue")) return YELLOW;
        if (entry.startsWith("Deleted")) return RED;
        if (entry.startsWith("Registered")) return BLUE;
        if (entry.startsWith("Sorted")) return GRAY;
        return CYAN;
    }

    static void showReport() {
        printTitle("Reports and Statistics");
        if (patientIds.isEmpty()) {
            printWarning("No patient records yet. Please register a patient first.");
            return;
        }

        int total = patientIds.size();
        int registered = 0, waiting = 0, called = 0, served = 0;
        int high = 0, medium = 0, low = 0;
        int highWaiting = 0;
        int manualReviews = 0;
        int totalMonths = 0;
        int youngestIndex = 0;
        int oldestIndex = 0;
        int[] symptomCounts = new int[SYMPTOM_NAMES.length];

        // One pass over all records to collect the numbers
        for (int i = 0; i < total; i++) {
            String status = statuses.get(i);
            switch (status) {
                case STATUS_REGISTERED -> registered++;
                case STATUS_WAITING -> waiting++;
                case STATUS_CALLED -> called++;
                case STATUS_SERVED -> served++;
                default -> { }
            }

            if (manualReviewFlags.get(i)) {
                manualReviews++;
            }
            String level = manualReviewFlags.get(i) ? "REVIEW" : priorityLabel(urgencyScores.get(i));
            switch (level) {
                case "HIGH" -> {
                    high++;
                    if (status.equals(STATUS_WAITING)) {
                        highWaiting++;
                    }
                }
                case "MEDIUM" -> medium++;
                case "LOW" -> low++;
                default -> { } // REVIEW is counted separately
            }

            int months = patientAgeMonths.get(i);
            totalMonths += months;
            if (months < patientAgeMonths.get(youngestIndex)) {
                youngestIndex = i;
            }
            if (months > patientAgeMonths.get(oldestIndex)) {
                oldestIndex = i;
            }

            // Count each checklist symptom (free-text "Other" is not counted)
            String[] parts = symptomTexts.get(i).split(", ");
            for (String part : parts) {
                for (int k = 0; k < SYMPTOM_NAMES.length; k++) {
                    if (k != SYM_OTHER && part.equals(SYMPTOM_NAMES[k])) {
                        symptomCounts[k]++;
                    }
                }
            }
        }

        // Most common symptom
        int topSymptom = 0;
        for (int k = 1; k < symptomCounts.length; k++) {
            if (symptomCounts[k] > symptomCounts[topSymptom]) {
                topSymptom = k;
            }
        }

        double averageMonths = (double) totalMonths / total;

        System.out.println(BOLD + BLUE + "PATIENTS" + RESET);
        printReportRow("Total records", "" + total);
        printReportRow("Registered (not queued)", "" + registered);
        printReportRow("Waiting in queue", "" + waiting);
        printReportRow("Called (consultation pending)", "" + called);
        printReportRow("Served (completed)", "" + served);
        System.out.println();

        System.out.println(BOLD + YELLOW + "PRIORITY" + RESET);
        printReportBar("HIGH", high, RED);
        printReportBar("MEDIUM", medium, YELLOW);
        printReportBar("LOW", low, GREEN);
        printReportRow("High priority still waiting", "" + highWaiting);
        printReportRow("Manual reviews required", "" + manualReviews);
        printInfo("HIGH/MEDIUM/LOW counts exclude REVIEW patients.");
        System.out.println();

        System.out.println(BOLD + MAGENTA + "AGE" + RESET);
        printReportRow("Youngest", formatAge(patientAgeMonths.get(youngestIndex))
                + "  (" + patientNames.get(youngestIndex) + ")");
        printReportRow("Oldest", formatAge(patientAgeMonths.get(oldestIndex))
                + "  (" + patientNames.get(oldestIndex) + ")");
        if (averageMonths < 24) {
            printReportRow("Average age", String.format("%.1f months", averageMonths));
        } else {
            printReportRow("Average age", String.format("%.1f years", averageMonths / 12.0));
        }
        System.out.println();

        System.out.println(BOLD + CYAN + "SYMPTOMS" + RESET);
        if (symptomCounts[topSymptom] == 0) {
            printReportRow("Most common symptom", "Not available");
        } else {
            printReportRow("Most common symptom", SYMPTOM_NAMES[topSymptom]
                    + "  (" + symptomCounts[topSymptom] + " patient(s))");
        }
    }

    static void printReportRow(String label, String value) {
        System.out.println("  " + GRAY + pad(label, 30) + RESET + BOLD + value + RESET);
    }

    // Prints a label with a small bar made of # characters
    static void printReportBar(String label, int count, String color) {
        System.out.println("  " + color + BOLD + pad(label, 10) + RESET
                + color + "#".repeat(count) + RESET + "  " + count);
    }

    // ===== 12. INPUT VALIDATION HELPERS =====

    // Keeps asking until the user enters a whole number from min to max
    static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(CYAN + "> " + RESET + prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value < min || value > max) {
                    printError("Please enter a number from " + min
                            + (max == Integer.MAX_VALUE ? " and above." : " to " + max + "."));
                } else {
                    return value;
                }
            } catch (NumberFormatException e) {
                printError("Invalid input. Please enter a whole number.");
            }
        }
    }

    // Asks for age in years (and months if under 1 year). Returns the age in MONTHS.
    static int readAgeInMonths() {
        int years = readInt("Age in years (enter 0 if under 1 year old): ", 0, 120);
        if (years == 0) {
            return readInt("Age in months (0-11, enter 0 if under 1 month): ", 0, 11);
        }
        return years * 12;
    }

    // Keeps asking until the user enters non-empty text
    static String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(CYAN + "> " + RESET + prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                printError("This field cannot be empty.");
            } else {
                return input;
            }
        }
    }

    private static void printPatientID(String patientID) {
        System.out.println(GRAY + patientID + RESET);
    }
}