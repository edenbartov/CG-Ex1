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
	private double[][] cost;
	private int[][] energy;
	BufferedImage gradientMag;
	final int PADDING = 2;

	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		// TODO : Include some additional initialization procedures.
		this.gradientMag = gradientMagnitude();
		cost = new double[this.inWidth + PADDING][this.inHeight + PADDING];
		energy = new int[this.inWidth + PADDING][this.inHeight + PADDING];
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
		throw new UnimplementedMethodException("carveImage");
	}

	private void energyCalc(){
		forEach((y,x) -> {
			this.energy[x+1][y+1] = new Color(gradientMag.getRGB(x,y)).getBlue();
		});
	}

	private void horizontalCostCalc(){
		forEach((y,x) -> {
			int i = x+1;
			int j = y+1;
			//base case for horizontal
	 		if(x==0){
				this.cost[i][j] = energy[i][j];
	 		}else{ // not the base case
				int cU = Math.abs(energy[i - 1][j] - energy[i][j - 1]) + Math.abs(energy[i - 1][j] - energy[i + 1][j]);
				int cH = Math.abs(energy[i - 1][j] - energy[i + 1][j]);
				int cD = Math.abs(energy[i + 1][j] - energy[i][j - 1]) + Math.abs(energy[i - 1][j] - energy[i + 1][j]);
				this.cost[i][j] = energy[i][j] +  Math.min(cost[i-1][j-1] + cU,Math.min(cost[i-1][j] + cH,cost[i-1][j+1] + cD));

			}
			});
		}
	private LinkedList<Coordinate> seamBacktrack(){
		double min_cost = Integer.MAX_VALUE;
		Coordinate first_index = new Coordinate(inWidth,0);
		for(int y = 1; y<=inHeight;y++){
			if (cost[inWidth][y]<min_cost) {
				min_cost = cost[inWidth][y];
				first_index = new Coordinate(inWidth-1,y-1);
			}
		}
		LinkedList<Coordinate> seam = new LinkedList<>();
		seam.add(first_index);
		Coordinate cur_coordinate = first_index;
		for (int x = inWidth-1; x >0 ; x--) {
			int old_y = cur_coordinate.Y;
			for (int y = old_y-1; y <= old_y+1 ; y++) {


			}



		}


		return null;
	}
	private void energyCalc(LinkedList<Coordinate> currentSeam){

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
