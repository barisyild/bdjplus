## BDJPlus

### What is this?
BDJPlus is bdjstack.jar bytecode patch project for jailbroken PS5 consoles.

### Motivation
Using homebrew content on PS5 consoles without BD writer, and creating a modular background services.

### What is benefits?
Thanks to the modular system, it is possible to create different usage areas, also you can create your own modules to the system, you can check the module repos for this.

### Requirements
- Jailbroken PS5 console
- A dumped bdjstack.jar from PS5 console
- system_ex write access
- BD-J supported disc (*Movie or Any BD-J supported disc)<br>

#### *Why do modules not work as they should?
Not every BD-J movie is fully compatible, if the BD-J movie is not compatible, you can make the incompatible discs compatible by creating a bypass.txt file in the "/system_ex/app/NPXS40140/cdc" folder and typing the incompatible movie disc keys into this folder. If you're buying BD-j movies, I suggest you buy a compatible disc like Warcraft: The Beginning.

### Installation
Copy the patched "bdjstack.jar" file to "/system_ex/app/NPXS40140/cdc/" on your console.

### Build Instructions (Windows)
1. Clone the repo
2. Copy the original "/system_ex/app/NPXS40140/cdc/bdjstack.jar" file from your console and name it "bdjstack.original.jar" and copy it to the project's home directory.
3. Start the build.bat file and wait for the build to finish.
4. Copy the generated "bdjstack.jar" file to "/system_ex/app/NPXS40140/cdc/" on your console.