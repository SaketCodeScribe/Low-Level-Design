package Problems;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Main {
    private static final int[][] DIRS = new int[][]{{-1,0},{1,0},{0,-1},{0,1},{1,1},{-1,1},{1,-1},{-1,-1}};

    public static void main(String[] args) {
        System.out.println(getMinInconvenience(new int[][]{{0,0,0,1},{0,0,0,1}}));
    }
    public static int getMinInconvenience(int[][] grid) {
        int m = grid.length, n = grid[0].length;

        int low = 0, high = bfs(grid, m, n), mid, ans = high;

        while(low <= high) {
            mid = low + (high-low)/2;

            if (isPossible(grid, m, n, mid)) {
                ans = mid;
                high = mid-1;
            } else low = mid+1;
        }
        return ans;
    }

    private static boolean isPossible(int[][] grid, int m, int n, int d) {
        int i, j, minR = m, maxR = -1, minC = n, maxC = -1;

        for(i=0; i<m; i++) {
            for(j=0; j<n; j++) {
                if (grid[i][j] > d) {
                    minR = Math.min(minR, i);
                    maxR = Math.max(maxR, i);
                    minC = Math.min(minC, j);
                    maxC = Math.max(maxC, j);
                }
            }
        }
        if (minR == m || minC == n || maxR == -1 || maxC == -1) return true;
        return maxR-minR <= 2*d && maxC - minC <= 2*d;
    }

    private static int bfs(int[][] grid, int m, int n) {
        int i, j, inconvenience = 0;
        boolean[][] vis = new boolean[m][n];
        Queue<int[]> queue = new LinkedList<>();

        for(i=0; i<m; i++) {
            for(j=0; j<n; j++) {
                if (grid[i][j] == 1) {
                    grid[i][j] = 0;
                    vis[i][j] = true;
                    queue.offer(new int[]{i,j, i, j});
                }
            }
        }
        if (queue.isEmpty()) {
            for(i=0; i<m; i++) Arrays.fill(grid[i], Math.max(m,n));
            return Math.max(m, n);
        }
        while(!queue.isEmpty()) {
            int[] node = queue.poll();
            i = node[0];
            j = node[1];
            int pi = node[2], pj = node[3];
            for(int[] dir:DIRS) {
                int x = i+dir[0], y = j+dir[1];
                if (x >= 0 && x < m && y >= 0 && y<n && !vis[x][y] && grid[x][y] == 0) {
                    grid[x][y] = Math.max(Math.abs(x-pi), Math.abs(y-pj));
                    inconvenience = Math.max(inconvenience, grid[x][y]);
                    vis[x][y] = true;
                    queue.offer(new int[]{x,y,pi,pj});
                }
            }
        }
        return inconvenience;
    }
}
