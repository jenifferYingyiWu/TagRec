/*
 TagRecommender:
 A framework to implement and evaluate algorithms for the recommendation
 of tags.
 Copyright (C) 2013 Dominik Kowald
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation, either version 3 of the
 License, or (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.
 
 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package engine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import file.BookmarkReader;

public class TagRecommenderEvalEngine implements EngineInterface {

	private EngineInterface lmEngine;
	private EngineInterface bllEngine;
	private EngineInterface threelEngine;
	
	//private Random random;
	private BufferedWriter bw;
	
	public TagRecommenderEvalEngine() {
		this.lmEngine = null;
		this.bllEngine = null;
		this.threelEngine = null;
		this.bw = null;
		//this.random = new Random();
		
		try {
			FileWriter writer = new FileWriter(new File("./data/tagrec_log.txt"), true);
			this.bw = new BufferedWriter(writer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void loadFile(String filename) throws Exception {
		this.lmEngine = null;
		this.bllEngine = null;
		this.threelEngine = null;
		
		BookmarkReader reader = new BookmarkReader(0, false);
		reader.readFile(filename);
		if (reader.getCategories().size() > 0) {
			this.threelEngine = new ThreeLayersEngine();
			this.threelEngine.loadFile(filename);
		}
		if (reader.hasTimestamp()) {
			this.bllEngine = new BaseLevelLearningEngine();
			this.bllEngine.loadFile(filename);
		}
		this.lmEngine = new LanguageModelEngine();
		this.lmEngine.loadFile(filename);
	}

	@Override
	public synchronized Map<String, Double> getEntitiesWithLikelihood(String user, String resource, List<String> topics, Integer count, Boolean filterOwnEntities, String algorithm) {
		Map<String, Double> returnMap = null;
		String algorithmString = null;
		boolean useCognitiveAlgo = (algorithm == null || !algorithm.equals("mp"));//this.random.nextBoolean();
		
		if (useCognitiveAlgo) {
			/*if (topics != null && topics.size() > 0 && this.threelEngine != null) {
				algorithmString = "3LT";
				returnMap = this.threelEngine.getEntitiesWithLikelihood(user, resource, topics, count);
			} else */if (this.bllEngine != null) {
				algorithmString = "BLL";
				returnMap = this.bllEngine.getEntitiesWithLikelihood(user, resource, topics, count, filterOwnEntities, algorithm);
			}
		}
		if (!useCognitiveAlgo || returnMap == null) {
			algorithmString = "MP";
			returnMap = this.lmEngine.getEntitiesWithLikelihood(user, resource, topics, count, filterOwnEntities, "mp_u_r");
		}

		if (this.bw != null) {
			try {
				this.bw.write(user + "|" + resource + "|" + topics + "|" + count + "|" + filterOwnEntities + "|" + System.currentTimeMillis() + "|" + useCognitiveAlgo + "|" + algorithmString + "|" + returnMap.keySet() + "\n");
				this.bw.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return returnMap;
	}
}