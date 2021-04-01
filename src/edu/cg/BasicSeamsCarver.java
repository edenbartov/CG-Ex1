package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;


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

	private long[][] cost; // The cost matrix (used for the dynamic programming)
	private long[][] energy; // Matrix for the energy values of the pixels
	private int[][] backtrack; // Helper matrix for backtracking the cost matrix when finding the optimal seam
	private Coordinate[][] indexMatrix; // Matrix which maps coordinates to the original pixels in the working image
	BufferedImage greyscaled; // The grey-scaled version of the image
	BufferedImage coloredSeamsImage; // Image to use for the showSeams functionality
	private int rows; // Number of current rows in the image (changes dynamically)
	private int cols; // Number of current columns in the image (changes dynamically)
	LinkedList<LinkedList<Coordinate>> horizontalSeams; // List of all horizontal seams
	LinkedList<LinkedList<Coordinate>> verticalSeams; // List of all vertical seams

	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		// TODO : Include some additional initialization procedures.
		this.greyscaled = greyscale();
		this.coloredSeamsImage = deepCopy();

		cost = new long[this.inWidth][this.inHeight];
		energy = new long[this.inWidth][this.inHeight];
		backtrack = new int[this.inWidth][this.inHeight];

		rows = this.inHeight;
		cols = this.inWidth;

		horizontalSeams = new LinkedList<>();
		verticalSeams = new LinkedList<>();

		indexMatrix = new Coordinate[cols][rows];
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				indexMatrix[x][y] = new Coordinate(x, y);
			}
		}
	}

	public BufferedImage carveImage(CarvingScheme carvingScheme) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
				// and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
				// Note you must consider the 'carvingScheme' parameter in your procedure.
				// Return the resulting image.

		switch (carvingScheme) {
			case VERTICAL_HORIZONTAL -> {
				for (int i = 0; i < numberOfVerticalSeamsToCarve; i++) {
					carveVertical();
				}
				for (int i = 0; i < numberOfHorizontalSeamsToCarve; i++) {
					carveHorizontal();
				}
			}
			case HORIZONTAL_VERTICAL -> {
				for (int i = 0; i < numberOfHorizontalSeamsToCarve; i++) {
					carveHorizontal();
				}
				for (int i = 0; i < numberOfVerticalSeamsToCarve; i++) {
					carveVertical();
				}
			}
			case INTERMITTENT -> {
				int tempVertical = numberOfVerticalSeamsToCarve;
				int tempHorizontal = numberOfHorizontalSeamsToCarve;
				while (tempVertical + tempHorizontal > 0) {
					if (tempVertical > 0) {
						carveVertical();
						tempVertical--;
					}
					if (tempHorizontal > 0) {
						carveHorizontal();
						tempHorizontal--;
					}
				}
			}
		}

		return downScaleImage();
	}

	/**
	 * Carve single horizontal seam
	 */
	private void carveHorizontal() {
		energyCalc();
		cost = new long[cols][rows];
		backtrack = new int[cols][rows];
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				if (x == 0) {
					cost[x][y] = energy[x][y];
				} else {
					horizontalCostCalc(this.cost, x, y);
				}
			}
		}

		LinkedList<Coordinate> seam = horizontalSeamBacktrack();
		rows--;
		newIndexMatrix(seam,false);
		seam = overrideSeam(seam);
		horizontalSeams.add(seam);
	}

	/**
	 * Carve single vertical seam
	 */
	private void carveVertical() {
		energyCalc();
		cost = new long[cols][rows];
		backtrack = new int[cols][rows];
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				if (y == 0) {
					cost[x][y] = energy[x][y];
				} else {
					verticalCostCalc(this.cost, x, y);
				}
			}
		}

		LinkedList<Coordinate> seam = verticalSeamBacktrack();
		cols--;
		newIndexMatrix(seam,true);
		seam = overrideSeam(seam);
		verticalSeams.add(seam);
	}

	/**
	 * Calculate the value of the cell (x, y) in the cost matrix using dynamic programming
	 * for horizontal seam carving
	 * @param cost
	 * @param x
	 * @param y
	 */
	private void horizontalCostCalc(long[][] cost, int x, int y) {
		long cU, cH, cD;
		if (y == 0){
			cU = Long.MAX_VALUE;
			cH = Long.MAX_VALUE;
			cD = Math.abs(energy[x][y + 1] - energy[x - 1][y]);
			cD += cost[x - 1][y + 1];

		} else if (y == rows - 1) {
			cU = Math.abs(energy[x][y - 1] - energy[x - 1][y]);
			cH = Long.MAX_VALUE;
			cD = Long.MAX_VALUE;
			cU += cost[x - 1][y - 1];
		} else {
			cU = Math.abs(energy[x][y - 1] - energy[x - 1][y]) + Math.abs(energy[x][y - 1] - energy[x][y + 1]);
			cH = Math.abs(energy[x][y - 1] - energy[x][y + 1]);
			cD = Math.abs(energy[x][y + 1] - energy[x - 1][y]) + Math.abs(energy[x][y - 1] - energy[x][y + 1]);

			cU += cost[x - 1][y - 1];
			cH += cost[x - 1][y];
			cD += cost[x - 1][y + 1];
		}

		setCost(x, y, cU, cH, cD);
	}

	/**
	 * Calculate the value of the cell (x, y) in the cost matrix using dynamic programming
	 * for vertical seam carving
	 * @param cost
	 * @param x
	 * @param y
	 */
	private void verticalCostCalc(long[][] cost, int x, int y) {
		long cR, cV, cL;
		if (x == cols - 1){
			cR = Long.MAX_VALUE;
			cV = Long.MAX_VALUE;
			cL = Math.abs(energy[x - 1][y] - energy[x][y - 1]);
			cL += cost[x - 1][y - 1];

		} else if(x == 0) {
			cR = Math.abs(energy[x + 1][y] - energy[x][y - 1]);
			cV = Long.MAX_VALUE;
			cL = Long.MAX_VALUE;
			cR += cost[x + 1][y - 1];

		} else {
			cR = Math.abs(energy[x + 1][y] - energy[x][y - 1]) + Math.abs(energy[x - 1][y] - energy[x + 1][y]);
			cV = Math.abs(energy[x - 1][y] - energy[x + 1][y]);
			cL = Math.abs(energy[x - 1][y] - energy[x][y - 1]) + Math.abs(energy[x - 1][y] - energy[x + 1][y]);

			cR += cost[x + 1][y - 1];
			cV += cost[x][y - 1];
			cL += cost[x - 1][y - 1];
		}

		setCost(x, y, cL, cV, cR);
	}

	/**
	 * Set the value of the call (x, y) in the cost matrix.
	 * Set the value of the backtrack matrix in cell (x, y) to -1 (up or left), 0 (horizontally or vertically),
	 * or -1 (down or right).
	 * @param x
	 * @param y
	 * @param uL - up/left
	 * @param hV - horizontally/vertically
	 * @param dR - down/right
	 */
	private void setCost(int x ,int y, long uL, long hV, long dR){
		long min = Math.min(dR, Math.min(hV, uL));
		if (min == uL) {
			backtrack[x][y] = -1;
		} else if (min == hV) {
			backtrack[x][y] = 0 ;
		} else {
			backtrack[x][y] = 1;
		}
		cost[x][y] = energy[x][y] + min;
	}

	/**
	 * Use backtracking technique to find the optimal horizontal seam from the cost matrix
	 * @return - the optimal seam as a linked list of coordinates
	 */
	private LinkedList<Coordinate> horizontalSeamBacktrack() {
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
			y += backtrack[x][y];
			current = new Coordinate(x - 1, y);
			seam.add(0, current);
		}
		return seam;
	}

	/**
	 * Use backtracking technique to find the optimal vertical seam from the cost matrix
	 * @return - the optimal seam as a linked list of coordinates
	 */
	private LinkedList<Coordinate> verticalSeamBacktrack() {
		LinkedList<Coordinate> seam = new LinkedList<>();
		int minIndex = 0;
		for (int x = 1; x < cols; x++) {
			if (cost[x][rows - 1] < cost[minIndex][rows - 1]) {
				minIndex = x;
			}
		}
		Coordinate current = new Coordinate(minIndex,rows - 1);
		seam.add(current);

		for (int y = rows - 1; y > 0; y--) {
			int x = current.X;
			x += backtrack[x][y];
			current = new Coordinate(x ,y - 1);
			seam.add(0, current);
		}
		return seam;
	}

	/**
	 * Return the color of the pixel (x, y) in the given image
	 * @param img - the image
	 * @param c - the coordinate
	 * @return - the color at the given index (as a Color object)
	 */
	private Color originalIndexColor(BufferedImage img, Coordinate c) {
		return new Color(img.getRGB(c.X, c.Y));
	}

	/**
	 * Calculate the new index matrix when factoring in the new seam that will be removed
	 * @param seam - the new seam
	 * @param vertical - true if the seam is vertical, false if horizontal
	 */
	private void newIndexMatrix(LinkedList<Coordinate> seam, boolean vertical) {
		Coordinate[][] tempIndexMatrix = new Coordinate[cols][rows];
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				if (vertical) {
					if (seam.get(y).X <= x) {
						tempIndexMatrix[x][y] = indexMatrix[x + 1][y];
					} else if (seam.get(y).X > x) {
						tempIndexMatrix[x][y] = indexMatrix[x][y];
					}
				} else {
					if (seam.get(x).Y <= y) {
						tempIndexMatrix[x][y] = indexMatrix[x][y + 1];
					} else if (seam.get(x).Y > y) {
						tempIndexMatrix[x][y] = indexMatrix[x][y];
					}
				}
			}
		}
		this.indexMatrix = tempIndexMatrix;
	}

	/**
	 * Calculate the energy matrix
	 */
	private void energyCalc() {
		long[][] newEnergy = new long[cols][rows];
		int nextX = 1;
		int nextY = 1;
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows - 1; y++) {
				if(x >= cols - 1) {
					nextX = -1;
				}
				if(y >= rows - 1) {
					nextY = -1;
				}
				int currentColor =  originalIndexColor(greyscaled, indexMatrix[x][y]).getBlue();
				int Dx = currentColor - originalIndexColor(greyscaled, indexMatrix[x + nextX][y]).getBlue();
				int Dy = currentColor - originalIndexColor(greyscaled, indexMatrix[x][y + nextY]).getBlue();
				int nextColor = Math.min((int) Math.sqrt((Math.pow(Dx,2) + Math.pow(Dy, 2)) / 2), 255);
				newEnergy[x][y] = nextColor;
			}
		}
		this.energy = newEnergy;
	}

	/**
	 * Clone the working image
	 * @return - the new copy
	 */
	private BufferedImage deepCopy () {
		BufferedImage newImage = newEmptyInputSizedImage();
		forEach((y, x) -> {
			newImage.setRGB(x, y, workingImage.getRGB(x, y));
		});
		return newImage;
	}

	/**
	 * Convert the coordinates of the seam to the corresponding coordinates in the original working image
	 * @param seam
	 * @return - the updated seam
	 */
	private LinkedList<Coordinate> overrideSeam(LinkedList<Coordinate> seam) {
		LinkedList<Coordinate> tempSeam = new LinkedList<>();
		for (Coordinate c : seam) {
			tempSeam.add(0, indexMatrix[c.X][c.Y]);
		}
		return tempSeam;
	}

	/**
	 * Colors a seam into the clone of the working image
	 * @param seam - the seam to color
	 * @param seamColorRGB - the color to use
	 */
	private void colorSeam(LinkedList<Coordinate> seam, int seamColorRGB) {
		for (Coordinate c : seam) {
			coloredSeamsImage.setRGB(c.X, c.Y, seamColorRGB);
		}
	}

	/**
	 * Scale the image down to the output size by copying the pixels corresponding to the remaining entries in the
	 * index matrix to a new BufferedImage
	 * @return - the new down-scaled image
	 */
	private BufferedImage downScaleImage() {
		BufferedImage result = newEmptyOutputSizedImage();
		System.out.println(cols - indexMatrix.length);
		System.out.println(rows - indexMatrix[0].length);
		for (int x = 0; x < indexMatrix.length; x++) {
			for (int y = 0; y < indexMatrix[0].length; y++) {
				Coordinate coordinate = indexMatrix[x][y];
				int color = workingImage.getRGB(coordinate.X, coordinate.Y);
				result.setRGB(x, y, color);
			}
		}

		return result;
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
		if (showVerticalSeams) {
			for (int i = 0; i < numberOfVerticalSeamsToCarve; i++) {
				carveVertical();
				for (LinkedList<Coordinate> seam : verticalSeams) {
					colorSeam(seam, seamColorRGB);
				}
			}
		} else {
			for (int i = 0; i < numberOfHorizontalSeamsToCarve; i++) {
				carveHorizontal();
				for (LinkedList<Coordinate> seam : horizontalSeams) {
					colorSeam(seam, seamColorRGB);
				}
			}
		}

		return coloredSeamsImage;
	}
}
