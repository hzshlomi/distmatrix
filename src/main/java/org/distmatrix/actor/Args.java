package org.distmatrix.actor;

@SuppressWarnings("SameParameterValue")
public class Args {

    public static final String DEF_POINTS_FILE_NAME = "default-3.points";

    private boolean usage = false;
    private Integer numWorkers = 4;
    private String pointsFileName = DEF_POINTS_FILE_NAME;

    public Args(String[] args) {

        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-w":
                        this.numWorkers = nextInt(args, "-w", i);
                        i++;
                        break;
                    case "-p":
                        this.pointsFileName = nextArg(args, "-p", i);
                        i++;
                        break;
                    case "-h":
                    case "--help":
                        this.usage = true;
                        break;
                }
            }
        }
    }

    final boolean usage() {
        return usage;
    }

    final void printUsage() {
        System.out.println("Usage:");
        System.out.println("\t'-w'        :\tspecify number of workers");
        System.out.println("\t'-p'        :\tpoints file path with one point per line in 'x,y' coordinate format");
        System.out.println("\t'-h|--help' :\tprint usage");
    }

    final Integer numWorkers() {
        return numWorkers;
    }

    final String pointsFileName() {
        return pointsFileName;
    }

    private String nextArg(String[] args, String arg, int i) {
        if ((i + 1) > (args.length - 1)) {
            throw new IllegalArgumentException("Missing value for argument: " + arg);
        }
        return args[i + 1];
    }

    private int nextInt(String[] args, String arg, int i) {
        final String nextArg = nextArg(args, arg, i);
        return Integer.parseInt(nextArg);
    }
}