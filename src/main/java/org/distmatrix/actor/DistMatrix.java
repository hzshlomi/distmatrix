package org.distmatrix.actor;

public class DistMatrix {

    public final double[][] mat;

    DistMatrix(int dim) {
        mat = new double[dim][dim];
        for (int i = 0; i < dim; i++) {
            mat[i][i] = 0;
        }
    }

    public void set(int i, int j, double dist) {
        if (i == j) throw new IllegalArgumentException("Distance matrix is hollow");
        if (dist < 0) throw new IllegalArgumentException("Distance must be non-negative");
        mat[i][j] = mat[j][i] = dist;
    }

    @Override
    public String toString() {
        return "DistMatrix{" +
                dumpMatrix(this.mat) +
                '}';
    }

    private StringBuilder dumpMatrix(final double[][] matrix) {
        final StringBuilder buf = new StringBuilder();
        buf.append("\n");
        for (double[] doubles : matrix) {
            buf.append("|");
            for (double aDouble : doubles) {
                buf.append(String.format(" %f |", aDouble));
            }
            buf.append("\n");
        }
        return buf;
    }
}
