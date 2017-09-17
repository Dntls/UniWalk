package simrank;

import java.io.IOException;
import java.util.Arrays;

import lxctools.FixedCacheMap;
import lxctools.Log;
import lxctools.StopWatch;
import structures.Graph;
import utils.Eval;
import utils.Print;
import conf.MyConfiguration;
/**
 * use FixedHashTable<Integer>[] sim
 * do not store all similarities for each vertex.
 * @author luoxiongcai
 *
 */
public class SingleRandomWalk_M {
	protected static final int topk = MyConfiguration.TOPK;
	protected int capacity;
	protected int STEP = 2;	
	protected int COUNT;
	protected Graph g;
	protected FixedCacheMap[] sim;
	protected int SAMPLE = 10000;
	public static double[] cache;  
	
	
	@SuppressWarnings("unchecked")
	public SingleRandomWalk_M(Graph g, int M, int sample, int step) {
		this.STEP = step;
		this.g = g;
		this.COUNT = g.getVCount();
		sim = new FixedCacheMap[1000];
		capacity = topk * M;
		SAMPLE = sample;
		for (int i = 0; i < 1000; i++){
			sim[i] = new FixedCacheMap(capacity);
		}
		
		cache = new double[STEP+1];
		for (int i = 1; i <= STEP; i++)
			cache[i] = Math.pow(MyConfiguration.C, i);
	}

	public void compute(){
		StopWatch.start();
		
//		for (int i = 0; i < 1000; i++){
//			if(i%10==0){
//				System.out.println(i);
//			}
//			walk(i, 2*STEP,0);
//		}
		for (int i = 0; i < Math.min(COUNT,1000); i++){
			if(i%100==0){
				System.out.println(i);
			}
			walk(i, 2*STEP,0);
		}
		StopWatch.say("finish");
	}
	
	/**
	 * 
	 * @param v : the start node of the random walk
	 * @param len : the length of the random walk
	 * @param initSample : already sampled count.for reuse other paths.
	 */
	protected void walk(int v, int len, int initSample){
		int maxStep =Math.max( 2 * STEP , len);
		for (int i = initSample; i < SAMPLE; i++) {
			int pathLen = 0;
			int[] path = new int[maxStep + 1];
			Arrays.fill(path, -1);
			path[0] = v;
			int cur = v;
			while (pathLen < maxStep){
				cur = g.randNeighbor(cur);
				if (cur == -1) break;
				path[++pathLen] = cur;
			}
			// compute the sim to v;
			computePathSim(path, pathLen);			
		}
	}
	

	
	/**
	 * compute the similarity of one path that starts from path[0]
	 * @param path : a path from path[0]
	 * @param pathLen : the length of the path. 
	 */
	protected void computePathSim(int[] path, int pathLen){
		if (pathLen == 0) return;
		int source = path[0];
		for (int i = 1 ; i <= STEP && 2 * i <= pathLen; i++){
			int interNode = path[i];
			int target = path[2*i];
			if (source == target) continue;
			if (isFirstMeet(path,0, 2*i)){
				double incre =  cache[i]* g.degree(interNode)/ g.degree(target) / SAMPLE;
				sim[source].put(target, (float)incre);	
			}
		}
	}
	
	public FixedCacheMap[] getResult(){
		return sim;
	}
	
	/**
	 * srcIndex < dstIndex
	 * @param path
	 * @param targetIndex
	 * @return
	 */
	public boolean isFirstMeet(int[] path, int srcIndex, int dstIndex){
		int internal = (dstIndex - srcIndex) / 2 + srcIndex;
		for (int i = srcIndex; i < internal; i++){
			if (path[i] == path[dstIndex - i + srcIndex]) return false;
		}
		return true;
	}

	public static void main(String[] args) throws IOException {
		int fileNum = MyConfiguration.fileNum;
		fileNum = 2;
		for(int i=0;i<fileNum;i++){
			// input path
			String graphInPath = MyConfiguration.in_u_u_graphPath[i];
			String goldPath = MyConfiguration.out_u_u_graphPath_simrank[i] + "_simrank_navie_top" + MyConfiguration.TOPK +".txt";
			
			// output
			String basePath = MyConfiguration.out_u_u_graphPath_single[i];
			String logPath = basePath + "_Single_M_Test.log";
			
			int[] samples = {10000};
			int[] steps = {5};
			
			Log log = new Log(logPath);
			log.info("################## Test_u_u_Top" + MyConfiguration.TOPK);
			System.out.println("read begin!");
			Graph g = new Graph(graphInPath, MyConfiguration.u_u_count[i]);
			System.out.println("read done!");
			StopWatch.say("read done!");
			for(int step: steps){
				for (int sample : samples){	// 这里step加上之后，路径还要进一步调整
					
					StopWatch.start();
					
					log.info("computation begin!");
					StopWatch.say("computation begin!");
					SingleRandomWalk_M srw = new SingleRandomWalk_M(g,5*1000,sample,step);
					srw.compute();
					StopWatch.say("computation done!");
					log.info("computation done!");

					System.out.println("第" + i + "个文件  Step:" + step + " Sample:"+sample + "TopK:" + 20);
					log.info("第" + i + "个文件  Step:" + step + " Sample:"+sample + "TopK:" + 20);
					String outPath = basePath + "_Single_M_top" + 20 + "_step" + step + "_sample" + sample + ".txt"; 
					log.info("u_u_graph singleRandomWalk output done!");
					String prePath = basePath + "_Single_M_top" + 20 + "_step" + step + "_sample" + sample + "precision.txt";

					// sim
					Print.printByOrder(srw.getResult(), outPath, MyConfiguration.TOPK);
					// precision
					log.info("printByOrder done!");
					String pre = Eval.precision(goldPath+".sim.txt", outPath+".sim.txt", prePath, 20);
					log.info("Basic SingleRamdomWalk_M Top" + MyConfiguration.TOPK + " step" + step + " sample" + sample + " precision: " + pre);
				}
			}
			g = null;
			log.close();
		}
	}

}
