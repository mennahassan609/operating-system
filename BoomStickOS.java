package main;

import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.util.*;

public class BoomStickOS {
	
	static int createdProcs = 0;
	static Queue<Integer> readyQueue = new LinkedList<Integer>();
	static String[] memory;
	
	public static void  printContentsOfMemory(){
		System.out.println("-----Queue-----------");
		printContentsOfQueue();
		System.out.println("-----Memory----------");
		for(int i = 0; i < 10; i++) {
			System.out.println(memory[i]);
		}
		System.out.println("-----DataSection---");
		for(int i = 10; i < 40; i++) {
			System.out.println(memory[i]);
		}
		System.out.println("-----Memory----------");
		
	}
	
	public static void printContentsOfQueue() {
		System.out.print("Ready Queue: ");
		for(int a : readyQueue) {
			System.out.print(a + ", ");
		}
		System.out.println();
	}
	
	public static String readFile(File f) throws FileNotFoundException {
		Scanner sc = new Scanner(f);
		String returnedFile = "";
		while(sc.hasNext()) {
			returnedFile = returnedFile + sc.nextLine() + "\n";
		}
		sc.close();
		return returnedFile;
	}
	
	public static void writeToDisk(String s, int procID) throws IOException {
		PrintWriter out = new PrintWriter(new File("C:\\Users\\youse\\BoomStickStorage\\" + procID + ".txt"));
		out.print(s);
		out.close();
	}
	
	public static void executeInstruction(String s, int id) {
		System.out.println("Executing     | " + id + " | " + s);
	}
	
	//Looking through memory for a space for the PCB in the reserved area
	public static int findFirstFreeSpaceInPCBBlock() {
		int l = 0;
		while(l < 10) {
			if(memory[l] == null) {
				return l;
			}
			l++;
		}
		return -1;
	}
	
	public static int findFirstFreeSpaceInMemoryToFitNewProcess(int requiredCellsInMemory) {
		int i = 10;
		int indexOfEmptySpaceBeginning = 10;
		int lengthOfCurrentEmptySpace = 1;
		boolean currentlyEmpty = false;
		boolean found = false;
		while(i < memory.length) {
			if(currentlyEmpty == true) {
				if(memory[i] != null) {
					currentlyEmpty = false;
					if(lengthOfCurrentEmptySpace > requiredCellsInMemory) {
						return indexOfEmptySpaceBeginning;
					}
				}
				else {
					if(lengthOfCurrentEmptySpace > requiredCellsInMemory) {
						return indexOfEmptySpaceBeginning;
					}
					lengthOfCurrentEmptySpace++;
				}
			}
			else {
				lengthOfCurrentEmptySpace = 0;
				if(memory[i] != null) {
					i++;
					continue;
				}
				else {
					currentlyEmpty = true;
					indexOfEmptySpaceBeginning = i;
					lengthOfCurrentEmptySpace++;
					i++;
					continue;
				}
			}
			i++;
		}
		if(lengthOfCurrentEmptySpace > requiredCellsInMemory) {
			return indexOfEmptySpaceBeginning;
		}
		return -1;
	}
	
	//This method takes the largest process it can out of the memory, encodes it into a string and then writes it to the disk
		public static void swapProcessOutToDisk() throws NumberFormatException, IOException {
			int startIndexOfLargestProcess = -1;
			int endIndexOfLargestProcess = -1;
			int lengthOfLargestProcess = -1;
			int indexOfPCBOfLargestProcess = -1;
			String[] PCBOfLargestProcComponents = new String[3];
			for(int i = 0; i < 10; i++) {
				if(memory[i] != null) {
					String PCB = memory[i];
					String[] PCBComponents = PCB.split("@");
					String ProcessBoundaries = PCBComponents[3];
					String[] ProcessBoundariesSplit = ProcessBoundaries.split("-");
					if((Integer.parseInt(ProcessBoundariesSplit[1]) - (Integer.parseInt(ProcessBoundariesSplit[0]))> lengthOfLargestProcess)){
						startIndexOfLargestProcess = Integer.parseInt(ProcessBoundariesSplit[0]);
						endIndexOfLargestProcess = Integer.parseInt(ProcessBoundariesSplit[1]);
						PCBOfLargestProcComponents = PCBComponents;
						lengthOfLargestProcess = endIndexOfLargestProcess - startIndexOfLargestProcess;
						indexOfPCBOfLargestProcess = i;
					}
				}
			}
			memory[indexOfPCBOfLargestProcess] = null;
			String newPCB = PCBOfLargestProcComponents[0] + "@" + PCBOfLargestProcComponents[1] + "@" + PCBOfLargestProcComponents[2] + "@" + PCBOfLargestProcComponents[3];
			String storedText = newPCB + "\n";
			for(int i = startIndexOfLargestProcess; i <= endIndexOfLargestProcess; i++) {
				storedText += memory[i] + "\n";
				memory[i] = null;
			}
			;
			writeToDisk(storedText, Integer.parseInt(PCBOfLargestProcComponents[0]));
			System.out.println("Succesfully wrote to disk the process " + Integer.parseInt(PCBOfLargestProcComponents[0]));
		}
	
	//This method reads the contents of the file path provided,
	//splits the file into lines and calculates how many cells will be required for all the lines of the program
	//Then writes a PCB string, assigning the process an ID, and starting it with PC = 0 and state = 1
	//It then calls findFirstFreeSpaceInPCBBlock and findFirstFreeSpaceInMemoryToFitNewProcess
	//If there is a free space in the PCB block and a big enough space in the memory for the process
	//it finds the location of the first free space in the memory large enough and sets that as the upper memory bound for the process
	//the lower bound is calculated as that index + the number of lines in the program + 3 to account for variables
	//Those bounds are used to finish writing the PCB.
	//The PCB is written to the previously mentioned spot and the lines of memory to the previously written spot as well as placeholders in the spaces for variables
	//However, if there isn't enough space in the memory for either the both the PCB and the memory,
	//The method calls swapProcessOutToDisk() until there's enough space available to do the above.
	//It then adds the ID of the new process to the readyQueue and increments the base ID.
	public static void createProcess(File f) throws NumberFormatException, IOException {
		System.out.println("Started creating a process");
		String programText = readFile(f);
		System.out.println("Finished reading the program file");
		String[] programLines = programText.split("\n");
		int requiredCellsInMemory = programLines.length + 3;
		String PCB = createdProcs + "@";
		
		int firstFreeSpaceInPCBBlock = findFirstFreeSpaceInPCBBlock();
		System.out.println("Found first free space in PCB block: " + firstFreeSpaceInPCBBlock);
		int firstFreeSpaceInMemoryForData = findFirstFreeSpaceInMemoryToFitNewProcess(requiredCellsInMemory);
		System.out.println("Found first free space in memory: " + firstFreeSpaceInMemoryForData);

		
		//If there's no space for the process pick processes and bring them out to disk until there's enough space
		if(firstFreeSpaceInMemoryForData == -1 || firstFreeSpaceInMemoryForData == -1) {
			while(true) {
				swapProcessOutToDisk();
				firstFreeSpaceInMemoryForData = findFirstFreeSpaceInMemoryToFitNewProcess(requiredCellsInMemory);
				if(firstFreeSpaceInMemoryForData != -1) break;
			}
		}
		//Placing PCB in PCB block and process in first available space in process block;
		if(firstFreeSpaceInMemoryForData >= 0 && firstFreeSpaceInPCBBlock >= 0) {
			int indexOfUpperBound = firstFreeSpaceInMemoryForData;
			int indexOfLowerBound = firstFreeSpaceInMemoryForData + requiredCellsInMemory - 1;
			PCB += indexOfUpperBound + "@1@" + indexOfUpperBound + "-" + indexOfLowerBound;
			memory[firstFreeSpaceInPCBBlock] = PCB;
			int k = 0;
			for(int j = indexOfUpperBound; j <= indexOfLowerBound; j++) {
				if(k >= programLines.length) {
					memory[j] = "Process " + createdProcs + " variable placeholder";
				}
				else {
					memory[j] = programLines[k];
					k++;
				}
			}
		}
		
		readyQueue.add(createdProcs);
		System.out.println("Finished creating new process of id " + createdProcs + "and added to the readyQueue");
		createdProcs++;
	}
	
	
	
	public static void scheduler(int cyclesPerProgram, boolean interruptable) throws IOException {
		while(!readyQueue.isEmpty()) {
			System.out.println("/////StartingSchedulerLoop/////");
			printContentsOfMemory();
			
			boolean returnToReady = true;
			boolean completed = false;
			
			//Selected a process for operation
			int processID = -1;
			if(readyQueue.peek() != null) {
				processID = readyQueue.remove();
				System.out.println("Scheduler     | " + processID + " | Selected for operation");
				;
			}
			else {
				System.out.println("Scheduler: No available procs for work");
				break;
			}
			
			//Retrieving that process's PCB data from the memory
			int programCounter = -1;
			int state = 0;
			int upperBoundary = -1;
			int lowerBoundary= -1;
			boolean found = false;
			for(int i = 0; i < 10; i++) {
				if(memory[i] != null) {
					String PCB = memory[i];
					String[] PCBComponents = PCB.split("@");
					if(Integer.parseInt(PCBComponents[0]) == processID && Integer.parseInt(PCBComponents[2]) != 4) {
						programCounter = Integer.parseInt(PCBComponents[1]);
						state = Integer.parseInt(PCBComponents[2]);
						String[] boundaries = PCBComponents[3].split("-");
						upperBoundary = Integer.parseInt(boundaries[0]);
						lowerBoundary = Integer.parseInt(boundaries[1]);
						found = true;
						break;
					}
				}
			}
			
			//Process not in memory; need to get it from the disk into memory first
			if(!found) {
				System.out.println("Couldn't find the process " + processID + "in memory");
				String programText = readFile(new File("C:\\Users\\youse\\BoomStickStorage\\" + processID + ".txt"));;
				String[] programLines = programText.split("\n");
				int requiredCellsInMemory = programLines.length - 1;
				String PCB = programLines[0];
				
				int firstFreeSpaceInPCBBlock = findFirstFreeSpaceInPCBBlock();
				int firstFreeSpaceInMemoryForData = findFirstFreeSpaceInMemoryToFitNewProcess(requiredCellsInMemory);

				
				//If there's no space for the process pick processes and bring them out to disk until there's enough space
				if(firstFreeSpaceInMemoryForData == -1 || firstFreeSpaceInMemoryForData == -1) {
					while(true) {
						firstFreeSpaceInMemoryForData = findFirstFreeSpaceInMemoryToFitNewProcess(requiredCellsInMemory);
						if(firstFreeSpaceInMemoryForData != -1) break;
						swapProcessOutToDisk();
					}
				}
				
				//Placing PCB in PCB block and process in first available space in process block;
				if(firstFreeSpaceInMemoryForData >= 0 && firstFreeSpaceInPCBBlock >= 0) {
					int indexOfUpperBound = firstFreeSpaceInMemoryForData;
					int indexOfLowerBound = firstFreeSpaceInMemoryForData + requiredCellsInMemory - 1;
					String[] PCBComponents = PCB.split("@");
					String[] boundaries = PCBComponents[3].split("-");
					int oldUpperBoundary = Integer.parseInt(boundaries[0]);
					int oldProgramCounter = Integer.parseInt(PCBComponents[1]);
					int newProgramCounter = oldProgramCounter - oldUpperBoundary + indexOfUpperBound;
					PCB = PCBComponents[0] + "@" + newProgramCounter + "@" + PCBComponents[2] + "@" + indexOfUpperBound + "-" + indexOfLowerBound;
					memory[firstFreeSpaceInPCBBlock] = PCB;
					int k = 1;
					for(int j = indexOfUpperBound; j <= indexOfLowerBound; j++) {
						memory[j] = programLines[k];
						k++;
					}
				}
				System.out.println("Process has been placed in memory: ");
				printContentsOfMemory();
			}
			
			int addressOfPCB = -1;
			for(int i = 0; i < 10; i++) {
				if(memory[i] != null) {
					String PCB = memory[i];
					String[] PCBComponents = PCB.split("@");
					if(Integer.parseInt(PCBComponents[0]) == processID) {
						programCounter = Integer.parseInt(PCBComponents[1]);
						state = Integer.parseInt(PCBComponents[2]);
						String[] boundaries = PCBComponents[3].split("-");
						upperBoundary = Integer.parseInt(boundaries[0]);
						lowerBoundary = Integer.parseInt(boundaries[1]);
						addressOfPCB = i;
						break;
					}
				}
			}
			
			
			
			//Executing the clock cycles worth of instructions
			for(int i = 0; i < cyclesPerProgram; i++) {
				if(programCounter >= lowerBoundary - 3) {
					System.out.println("Scheduler     | " + processID + " | Completed");
					returnToReady = false;
					break;
				}
				executeInstruction(memory[programCounter], processID);
				programCounter++;
			}
			
			
			
			if(returnToReady) {
				String reconstructedPCB = processID + "@" + programCounter + "@" + state + "@" + upperBoundary + "-" + lowerBoundary;
				memory[addressOfPCB] = reconstructedPCB;
				readyQueue.add(processID);
				System.out.println("Scheduler     | " + processID + " | Returned to back of ready queue");
				;
			}
			
			
		
			if(interruptable) {
				Scanner sc = new Scanner(System.in);
				System.out.println("Would you like to add a new process? (Y/N)");
				if(sc.nextLine().equals("Y")) {
					System.out.println("Please enter the new program's directory");
					createProcess(new File(sc.nextLine()));
					System.out.println("Added a new process to memory.");
					printContentsOfMemory();
				}
				System.out.println("Are you going to add more processes in the future? (Y/N)");
				if(!sc.nextLine().equals("Y")) {
					interruptable = false;
				}
			}
			
			if(readyQueue.isEmpty()) {
				System.out.println("Scheduler     | No more processes in queues.");
				break;
				}
			}
	}
	
	public static void main(String[] args) throws IOException {		
		Scanner sc = new Scanner(System.in);
		System.out.println("Creating memory...");
		memory = new String[40];
		
		System.out.println("How many instructions per round-robin cycle?");
		int cycles = Integer.parseInt(sc.nextLine());

		System.out.println("Kickstarting scheduler...");
		System.out.println("Enter the first program's directory: ");
		createProcess(new File(sc.nextLine()));
		System.out.println("Do you plan on interrupting the scheduler and adding more process during operation? (Y/N)");
		if(sc.nextLine().equals("Y")) {
			scheduler(cycles, true);
		}
		else {
			scheduler(cycles, false);
		}
		System.out.println("Exiting");
	}
}
