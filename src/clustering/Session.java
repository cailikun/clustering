package clustering;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import dynamicText.DynamicTextVector;
import dynamicText.TextVector;
import sementicAccurate.Word;

public class Session implements Comparable<Session>{
	public static Map<Integer,Session> ALLSESSIONS=new HashMap<Integer,Session>();
	public static Queue<Integer> LASTESTSESSION=new LinkedList<Integer>();
	private static int num=0;
	private Date latestTime;
	private Date startTime=new Date();
	private int id;
	public Map<String,Double> centralVector=new HashMap<String,Double>();
	public List<Integer> vectors=new ArrayList<Integer>();
	private Map<String,Integer> originalMap=new HashMap<String,Integer>();
	private Map<String,Double> tfidfMap=new HashMap<String,Double>();
	public Set<String> words=new HashSet<String>();//可视化阶段用到，用来存储关键词。
	public Map<String, Double> getTfidfMap() {
		return tfidfMap;
	}
	public Session() {
		// TODO Auto-generated constructor stub
		this.id=num++;
		try {
			latestTime=(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse("1970-1-1 00:00:00");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}
	public Date getLatestTime() {
		return latestTime;
	}
	/**
	 * 根据会话包含的消息向量列表，组成会话的原始消息向量
	 */
	public void createOriginalMap(){
		for(Integer id:vectors){
			TextVector vector=TextVector.ALLTEXTVECTORS.get(id);
			for(Entry<String, Integer> entry:vector.getWords().entrySet()){
				if(originalMap.containsKey(entry.getKey())){
					originalMap.put(entry.getKey(), originalMap.get(entry.getKey())+1);
				}
				else{
					originalMap.put(entry.getKey(), 1);
				}
			}
		}
	}
	public Map<String, Integer> getOriginalMap() {
		return originalMap;
	}
	/**
	 * 根据会话的原始向量，组建TFIDF权重向量，在所有的会话都组建完成原始向量且更新了WORD中的两个静态MAP之后运行.运行结束后清空原始向量
	 */
	public void createTIDFMap(){
		double totalWordNum=0;
		for(Integer value:originalMap.values())
			totalWordNum+=value;
		for(String key:originalMap.keySet()){
			Integer value=originalMap.get(key);
			double tf=value/totalWordNum;
			double idf=Math.log(Word.totalTextNum/(double)Word.WORDIDF.get(key)+0.01);
			tfidfMap.put(key, tf*idf);
		}
		double der=0;
		for(Double value:tfidfMap.values()){
			der+=value*value;
		}
		der=Math.sqrt(der);
		for(Entry<String,Double> entry:tfidfMap.entrySet()){
			tfidfMap.put(entry.getKey(), entry.getValue()/der);
		}
		originalMap.clear();
	}
	/**
	 * 重新计算会话的最新时间
	 */
	public void refreshLatestDate(){
		for(Integer id:vectors){
			TextVector tv=TextVector.ALLTEXTVECTORS.get(id);
			if(this.latestTime.before(tv.getDatetime())){
				latestTime=tv.getDatetime();
			}
		}
	}
	public void addVector(TextVector tv){
		vectors.add(tv.getId());
		if(tv.getDatetime().before(startTime))
			startTime=tv.getDatetime();
		refreshLatestDate();
	}
	public double getMaxSimilarity(TextVector tv){
		double max=0;
		int i;
		double diffTime2StartTime=(double)((tv.getDatetime().getTime()-startTime.getTime())/Threshold.hour1Toms);
		double s=12/diffTime2StartTime;
		if(vectors.size()>Threshold.newsetvector)
			i=Threshold.newsetvector;
		else i=0;
		for(;i<vectors.size();i++){
			TextVector vector=TextVector.ALLTEXTVECTORS.get(vectors.get(i));
			double similarity=DynamicTextVector.getDynamicSimilarity(tv.getTfidfVector(), vector.getTfidfVector());
			if(similarity>max)
				max=similarity;
		}
		return s*max;
	}
	/**
	 * 获取两个是时间的时间差是否大于
	 * @param time1 稍晚的时间
	 * @param time2 稍早的时间
	 * @return 时间差大于24H返回false，否则返回true
	 */
	public static boolean getDiffTime(Date time1,Date time2){
		long diffTime=time1.getTime()-time2.getTime();
		if(diffTime>Threshold.maxduration){
			return false;
		}
		else return true;
	}
	//获取ALLSESSIONS中最新的会话，即与TV时间相差在12小时内的会话
	public static List<Session> getLastestSessions(TextVector tv){
		Date date=tv.getDatetime();
		List<Session> list=new ArrayList<Session>();
		for(Session session:ALLSESSIONS.values()){
			if(date.getTime()-session.getLatestTime().getTime()<Threshold.maxduration){
				list.add(session);
			}
		}
		Collections.sort(list);
		List<Session> result=new ArrayList<Session>();
		for(Session s:list){
			if(result.size()<Threshold.k)
				result.add(s);
			else
				break;
		}
		return result;
	}
	@Override
	public int compareTo(Session o) {
		// TODO Auto-generated method stub
		if(this.latestTime.after(o.latestTime))
			return -1;
		else if(this.latestTime.before(o.latestTime))
			return 1;
		else
			return 0;
	}
	
	
	/**
	 * 以下三个方法是第一版本的会话抽取算法的需求，V1.2中取消
	 */
	/**
	 * 重新计算会话的中心向量，在第一版本的会话抽取算法中应用到
	 */
//	public void refreshCenter(){
//		centralVector.clear();
//		//存储本会话中的所有的TFIDF向量
//		List<Map<String,Double>> list=new ArrayList<Map<String,Double>>();
//		//存放会话中的所有向量包含的所有词语
//		Set<String> words=new HashSet<String>();
//		for(Integer id:vectors){
//			list.add(TextVector.ALLTEXTVECTORS.get(id).getTfidfVector());
//		}
//		for(Map<String,Double> map:list){
//			for(String word:map.keySet()){
//				words.add(word);
//				centralVector.put(word, 0.0);
//			}
//		}
//		for(String word:words){
//			for(Map<String,Double> map:list){
//				if(map.containsKey(word)){
//					centralVector.put(word, centralVector.get(word)+map.get(word));
//				}
//				else continue;
//			}
//		}
//		double num=list.size();
//		for(Entry<String, Double> entry:centralVector.entrySet()){
//			centralVector.put(entry.getKey(), entry.getValue()/num);
//		}
//	}
//	public void addVector(TextVector tv){
//		List<Integer> list=tv.getAllChildId();
//		vectors.addAll(list);
//		refreshCenter();
//		refreshLatestDate();
//	}
//	/**
//	 * 移除向量及其后续向量
//	 * @param tv
//	 */
//	public void removeVector(TextVector tv){
//		List<Integer> list=tv.getAllChildId();
//		vectors.removeAll(list);
//		refreshCenter();
//		refreshLatestDate();
	
}
