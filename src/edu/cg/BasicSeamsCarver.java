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
	private long[][] cost;
	private long[][] energy;
	private int[][] backtrack;
	private Coordinate[][] indexMatrix;
	BufferedImage gradientMag;
	BufferedImage greyscaled;
	BufferedImage coloredSeamsImage;
	private int rows;
	private int cols;
	LinkedList<LinkedList<Coordinate>> horizontalSeams;

	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		// TODO : Include some additional initialization procedures.
		this.gradientMag = gradientMagnitude();
		this.greyscaled = greyscale();
		this.coloredSeamsImage = deepCopy();

		cost = new long[this.inWidth][this.inHeight];
		energy = new long[this.inWidth][this.inHeight];
		backtrack = new int[this.inWidth][this.inHeight];
		rows = this.inHeight;
		cols = this.inWidth;

		indexMatrix = new Coordinate[cols][rows];
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				indexMatrix[x][y] = new Coordinate(x, y);
			}
		}

		horizontalSeams = new LinkedList<>();

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

		switch (carvingScheme) {
			case VERTICAL_HORIZONTAL: {
				break;
			}
			case HORIZONTAL_VERTICAL: {
				for (int i = 0; i < numberOfHorizontalSeamsToCarve; i++) {
					carveHorizontal();
				}
				break;
			}
			case INTERMITTENT: {

			}
		}

		return downScaleImage();
	}

	private void carveHorizontal() {
		energyCalc();
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

		LinkedList<Coordinate> seam = seamBacktrack();
		rows--;
		calcNewIndexMatrix(seam);
		seam = overrideSeam(seam);
		horizontalSeams.add(seam);
	}

	private void horizontalCostCalc(long[][] cost, int x, int y) {
		long cU, cH, cD;
		if(y==0){
			cU = Long.MAX_VALUE;
			cH = Long.MAX_VALUE;
			cD = Math.abs(energy[x][y+1] - energy[x-1][y]);
			cD += cost[x-1][y+1];

		}else if(y==rows -1 ){
			cU = Math.abs(energy[x][y - 1] - energy[x - 1][y]);
			cH = Long.MAX_VALUE;
			cD = Long.MAX_VALUE;
			cU += cost[x-1][y-1];
		}else {
			cU = Math.abs(energy[x][y - 1] - energy[x - 1][y]) + Math.abs(energy[x][y - 1] - energy[x][y + 1]);
			cH = Math.abs(energy[x][y - 1] - energy[x][y + 1]);
			cD = Math.abs(energy[x][y + 1] - energy[x - 1][y]) + Math.abs(energy[x][y - 1] - energy[x][y + 1]);

			cU += cost[x-1][y-1];
			cH += cost[x-1][y];
			cD += cost[x-1][y+1];
		}


		long min = Math.min(cU, Math.min(cH, cD));
		if(min == cU){
			backtrack[x][y] = -1;
		}else if(min == cH){
			backtrack[x][y] = 0 ;
		}else {
			backtrack[x][y] = 1;
		}
		cost[x][y] = energy[x][y] + min;

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
			y += backtrack[x][y];
			current = new Coordinate(x - 1, y);
			seam.add(0, current);
		}
		return seam;
	}

	private Color originalIndexColor(BufferedImage img, Coordinate c) {
		return new Color(img.getRGB(c.X, c.Y));
	}

	private void calcNewIndexMatrix(LinkedList<Coordinate> seam) {
		Coordinate[][] tempIndexMatrix = new Coordinate[cols][rows];
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				if (seam.get(x).Y <= y) {
					tempIndexMatrix[x][y] = indexMatrix[x][y+1];
				} else if (seam.get(x).Y > y) {
					tempIndexMatrix[x][y] = indexMatrix[x][y];
				} else {
					System.out.println(x + ", " + y);
				}
			}
		}
		this.indexMatrix = tempIndexMatrix;
	}

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

	private BufferedImage deepCopy () {
		BufferedImage newImage = newEmptyInputSizedImage();
		forEach((y, x) -> {
			newImage.setRGB(x, y, workingImage.getRGB(x, y));
		});
		return newImage;
	}

	private LinkedList<Coordinate> overrideSeam(LinkedList<Coordinate> seam) {
		LinkedList<Coordinate> tempSeam = new LinkedList<>();
		for (Coordinate c : seam) {
			tempSeam.add(0, indexMatrix[c.X][c.Y]);
		}
		return tempSeam;
	}

	private void colorSeam(LinkedList<Coordinate> seam, int seamColorRGB) {
		for (Coordinate c : seam) {
			coloredSeamsImage.setRGB(c.X, c.Y, seamColorRGB);
		}
	}

	private BufferedImage downScaleImage() {
		BufferedImage result = newEmptyOutputSizedImage();
		System.out.println(cols - indexMatrix.length);
		System.out.println(rows - indexMatrix[0].length);
		for (int x = 0; x < indexMatrix.length; x++) {
			for (int y = 0; y < indexMatrix[0].length; y++) {
				Coordinate coordinate = indexMatrix[x][y];
				if (coordinate == null) {
					System.out.println("null:" + x + ", " + y);
				}
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
