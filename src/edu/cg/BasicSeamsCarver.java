package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.SplittableRandom;


public class BasicSeamsCarver extends ImageProcessor {
	
	// An enum describing the carving scheme used by the seams carver.
	// VERTICAL_HORIZONTAL means vertical seams are removed first.
	// HORIZONTAL_VERTICAL means horizontal seams are removed first.
	// INTERMITTENT means seams are removed intermittently : vertical, horizontal, vertical, horizontal etc.
	public static enum CarvingScheme {
		VERTICAL_HORIZONTAL("Vertical seams first"),
		HORIZONTAL_VERTICAL("Horizontal seams first"),
		INTERMITTENT("Intermittent carving");
		
		public final String description;
		
		private CarvingScheme(String description) {
			this.description = description;
		}
	}
	
	// A simple coordinate class which assists the implementation.
	protected class Coordinate{
		public int X;
		public int Y;
		public Coordinate(int X, int Y) {
			this.X = X;
			this.Y = Y;
		}
	}
	
	// TODO :  Decide on the fields your BasicSeamsCarver should include. Refer to the recitation and homework 
			// instructions PDF to make an educated decision.
	private int[][] seams;
	private long[][] cost;
	private long[][] energy;
	BufferedImage gradientMag;
	private int rows;
	private int cols;

	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		// TODO : Include some additional initialization procedures.
		this.gradientMag = gradientMagnitude();
		cost = new long[this.inWidth][this.inHeight];
		energy = new long[this.inWidth][this.inHeight];
		rows = this.inHeight;
		cols = this.inWidth;

	}
	// TODO : calculate energy on all the pixels
		// TODO : dynamicly calculate min path
	// TODO : calculate energy on only removed seam the pixels
		// TODO : update cost metrix only removed seam the pixels


	public BufferedImage carveImage(CarvingScheme carvingScheme) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
				// and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
				// Note you must consider the 'carvingScheme' parameter in your procedure.
				// Return the resulting image.
		energyCalc();
		for (int x = 0; x < cols - 1; x++) {
			for (int y = 0; y < cols; y++) {
				try {
					if (x == 0) {
						cost[x][y] = energy[x][y];
					} else {
						cost[x][y] = energy[x][y] + horizontalCostCalc(x, y);
					}
				} catch (Exception e) {
					logger.log("carveImage");
					System.out.println(x + ", " + y);
				}
			}
		}

		LinkedList<Coordinate> seam = seamBacktrack();
		BufferedImage carvedImage = workingImage;
		for (Coordinate c : seam) {
			try {
				carvedImage.setRGB(c.X, c.Y, 0x000F);
			} catch (Exception e) {
				logger.log("carveImage");
				System.out.println(c.X + ", " + c.Y);
			}

		}
		return carvedImage;
	}

	private void energyCalc() {
		forEach((y, x) -> {
			this.energy[x][y] = new Color(gradientMag.getRGB(x, y)).getBlue();
		});
	}

	private long horizontalCostCalc(int x, int y) {
		long cU, cH, cD;
		if (y == 0) {
			cU = Long.MAX_VALUE;
			cH = cost[x - 1][y];
			cD = Math.abs(energy[x][y + 1] - energy[x - 1][y]) + cost[x - 1][y + 1];
		} else if (y == cost[0].length - 1) {
			cU = Math.abs(energy[x][y - 1] - energy[x - 1][y]) + cost[x - 1][y - 1];
			cH = cost[x - 1][y];
			cD = Long.MAX_VALUE;
		} else {
			cU = Math.abs(energy[x - 1][y] - energy[x][y - 1]) + Math.abs(energy[x - 1][y] - energy[x + 1][y]);
			cH = Math.abs(energy[x - 1][y] - energy[x + 1][y]);
			cD = Math.abs(energy[x + 1][y] - energy[x][y - 1]) + Math.abs(energy[x - 1][y] - energy[x + 1][y]);
		}
		return Math.min(cU, Math.min(cH, cD));
	}

	private LinkedList<Coordinate> seamBacktrack() {
		LinkedList<Coordinate> seam = new LinkedList<>();
		int minIndex = 0;
		for (int y = 1; y < rows; y++) {
			if (cost[cols - 1][y] < cost[cols - 1][minIndex]) {
				minIndex = y;
			}
		}

		Coordinate current = new Coordinate(cols - 1, minIndex);
		seam.add(current);

		for (int x = cols - 1; x > 0; x--) {
			int y = current.Y;
			long mid = cost[x - 1][y];
			long up = y > 0 ? cost[x - 1][y - 1] : Long.MAX_VALUE;
			long down = y < rows - 1 ? cost[x - 1][y + 1] : Long.MAX_VALUE;
			if (up < mid && up < down) {
				y--;
			} else if (down < mid && down < up) {
				y++;
			}
			current = new Coordinate(x - 1, y);
			seam.add(0, current);
		}
		return seam;
	}

	private void refactorMatrix(long[][] m, LinkedList<Coordinate> seam) {
		long[][] newCost = new long[cols][rows - 1];
		long[][] newEnergy = new long[cols][rows - 1];
		for (int x = 0; x < m.length; x++) {
			for (int y = 0; y < m[0].length - 1; y++) {
				if (y < seam.get(x).Y) {
					newCost[x][y] = m[x][y];
					newEnergy[x][y] = m[x][y];
				} else if (y > seam.get(x).Y) {
					newCost[x][y] = m[x][y + 1];
					newEnergy[x][y] = m[x][y + 1];
				}
			}
		}
		this.cost = newCost;
		this.energy = newEnergy;
		rows--;
	}

	private void energyCalc(LinkedList<Coordinate> currentSeam){
		BufferedImage newWorkingImage = greyscale();
		for (Coordinate c : currentSeam) {
			int x = c.X;
			int y = c.Y;
			int prevY = y - 1;
			int nextY = y + 1;
			if (prevY >= 0 && nextY <= rows - 1) {
				int currentColor =  new Color(newWorkingImage.getRGB(x, prevY)).getBlue();
				int Dy = currentColor - new Color(newWorkingImage.getRGB(x, nextY)).getBlue();
			}
			



//			int nextX = 1;
//			int nextY = 1;
//
//			if(x >= inWidth - 1) {
//				nextX = -1;
//			}
//			if(y >= inHeight - 1) {
//				nextY = -1;
//			}
//			int currentColor =  new Color(newWorkingImage.getRGB(x, y)).getBlue();
//			int Dx = currentColor - new Color(newWorkingImage.getRGB(x + nextX, y)).getBlue();
//			int Dy = currentColor - new Color(newWorkingImage.getRGB(x, y + nextY)).getBlue();
//			int nextColor = Math.min((int) Math.sqrt((Math.pow(Dx,2) + Math.pow(Dy, 2)) / 2), 255);
//			Color color = new Color(nextColor,nextColor,nextColor);
//			ans.setRGB(x, y, color.getRGB());
		}

	}

	private void costCalc(LinkedList<Coordinate> currentSeam){

	}

	public BufferedImage showSeams(boolean showVerticalSeams, int seamColorRGB) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Present either vertical or horizontal seams on the input image.
				// If showVerticalSeams = true, carve 'numberOfVerticalSeamsToCarve' vertical seams from the image.
				// Then, generate a new image from the input image in which you mark all of the vertical seams that
				// were chosen in the Seam Carving process. 
				// This is done by painting each pixel in each seam with 'seamColorRGB' (overriding its' previous value). 
				// Similarly, if showVerticalSeams = false, carve 'numberOfHorizontalSeamsToCarve' horizontal seams
				// from the image.
				// Then, generate a new image from the input image in which you mark all of the horizontal seams that
				// were chosen in the Seam Carving process.
		throw new UnimplementedMethodException("showSeams");
	}
}
