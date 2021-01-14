package aoki_eita1130;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

/**
 * Sample implementation of snake bot
 */
public class Aoki_eita1130 implements Bot {
	//どうやら1ゲームごとにBotが初期化されたりはしないらしい
	//なのでturnをインクリメントして覚えたりとかもできないので、
	//Bot直下になるべく変数はおかないほうがよさげ

	static boolean useMyPrint=false;

	static int superWinner=-1;//明示的にapple量で買っている状態

	static void myprint(Object o) {
		if(useMyPrint)System.err.print(o);
	}
	static void myprintln(Object o) {
		if(useMyPrint)System.err.println(o);
	}
	static void myprintln() {
		if(useMyPrint)System.err.println();
	}

	static final Coordinate mazeSize=new Coordinate(14,14);
	static Random rnd=new Random(0);
	static Coordinate orgApple=new Coordinate(-1,-1);
	static boolean eqCoord(Coordinate co,int x,int y) {
		if(co==null)return false;
		return (co.x==x&&co.y==y) ;
	}
	static class HashEntry{
		//long HashVal;		// ハッシュ値
		Direction Best;			// 前回の反復深化での最善手
		Direction Second;			// 前々回以前の反復深化での最善手
		long value;			// αβ探索で得た局面の評価値
		FLAG flag;			// αβ探索で得た値が、局面の評価値そのものか、上限値か下限値か
		//int Tesu;			// αβ探索を行った際の手数
		int depth;		// αβ探索を行った際の深さ
		//short remainDepth;	// αβ探索を行った際の残り深さ
		public HashEntry() {};
	}
	static Map<Long, HashEntry> HashTbl = new HashMap<>();
	//各ターンで初期化するもの
	static void initTurn(Coordinate apple) {
		HashTbl.clear();
		Aoki_eita1130.startTime=System.currentTimeMillis();
		Aoki_eita1130.akirame=false;
		Aoki_eita1130.orgApple=new Coordinate(apple.x,apple.y);
	}

	static class Hash{

		static final long[][] myBody;
		static final long[][] oppBody;
		static final long[][] myHead;
		static final long[][] oppHead;
		static final long[][] apple;
		static {
			myprintln("Hash init()");
			myBody=new long[Aoki_eita1130.mazeSize.y][Aoki_eita1130.mazeSize.x];
			oppBody=new long[Aoki_eita1130.mazeSize.y][Aoki_eita1130.mazeSize.x];
			myHead=new long[Aoki_eita1130.mazeSize.y][Aoki_eita1130.mazeSize.x];
			oppHead=new long[Aoki_eita1130.mazeSize.y][Aoki_eita1130.mazeSize.x];
			apple=new long[Aoki_eita1130.mazeSize.y][Aoki_eita1130.mazeSize.x];


			for(int y = 0; y< Aoki_eita1130.mazeSize.y; y++) {
				for(int x = 0; x< Aoki_eita1130.mazeSize.x; x++) {
					myBody[y][x]=rnd.nextLong();
					oppBody[y][x]=rnd.nextLong();
					myHead[y][x]=rnd.nextLong();
					oppHead[y][x]=rnd.nextLong();
					apple[y][x]=rnd.nextLong();

				}
			}
		}
	}
	enum FLAG{
		EXACTLY_VALUE,		// 値は局面の評価値そのもの
		LOWER_BOUND,		// 値は局面の評価値の下限値(val>=β)
		UPPER_BOUND			// 値は局面の評価値の上限値(val<=α)
	}
	class BB{
		int[]bit;
		final long filter=(1<<mazeSize.x)-1;

		public BB(int[] bit) {
			this.bit=bit.clone();
		}
		public BB() {
			bit=new int[mazeSize.y];
		}
		public void set(int x,int y) {
			long tmp=1<<x;
			this.bit[y]|=tmp;
		}
		public void set(Coordinate co) {
			set(co.x,co.y);
		}
		public void remove(int x,int y) {
			long tmp=( ~(1<<x) )&filter;
			this.bit[y]&=tmp;
		}
		public void remove(Coordinate co) {
			remove(co.x,co.y);
		}

		public boolean isBit(int x,int y) {
			long tmp=this.bit[y]>>x;
			return ((tmp&1)==1);
		}
		public boolean isBit(Coordinate co) {
			if(co==null)return false;
			return isBit(co.x,co.y);
		}

		public BB clone() {
			return new BB(this.bit);
		}


		public void print() {
			for(int x=0;x<mazeSize.x;x++) {

				myprint('-');
			}

			myprintln();
			for(int y=0;y<mazeSize.y;y++) {
				for(int x=0;x<mazeSize.x;x++) {

					long val=(this.bit[y]>>x)&1L;
					char ch=val==0?'.':'1';
					myprint(ch);
				}

				myprintln();
			}
		}
	}

	class RetVal{
		int S;
		int appleDistance;
		int orgAppleDistance;
		public RetVal() {
			S=0;
			appleDistance=-1;
			orgAppleDistance=-1;
		}
	}

	class BitMaze{
		BB bit;
		Snake[] snakes;
		Coordinate apple;
		int appleEater=-1;
		long hash=0;
		int eateDepth=100;
		Long[] shallowScore=new Long[] {null,null};

		private void setHash() {
			hash=0;
			for(Coordinate co:this.snakes[0].body) {
				if(co.equals(snakes[0].getHead())) {
					hash^=Hash.myHead[co.y][co.x];
				}else {
					hash^=Hash.myBody[co.y][co.x];
				}
			}
			for(Coordinate co:this.snakes[1].body) {
				if(co.equals(snakes[1].getHead())) {
					hash^=Hash.oppHead[co.y][co.x];
				}else {
					hash^=Hash.oppBody[co.y][co.x];
				}
			}
			if(apple!=null) {
				hash^=Hash.apple[apple.y][apple.x];
			}
		}

		public BitMaze(Snake my,Snake opp, Coordinate apple) {
			this.bit=new BB();
			snakes=new Snake[2];
			snakes[0]=my.clone();
			snakes[1]=opp.clone();
			this.apple=new Coordinate(apple.x,apple.y);
			for(int i=0;i<2;i++) {
				for(Coordinate co:this.snakes[i].body) {
					bit.set(co);
				}
			}
			this.setHash();
		}
		public BitMaze(BB bit,Snake[] snakes, Coordinate apple,int appleEater,long hash,int eateDepth,Long[]shallowScore ) {
			this.bit=bit;
			this.snakes=snakes;
			this.apple=apple;
			this.appleEater=appleEater;
			this.hash=hash;
			this.shallowScore=new Long[2];
			this.eateDepth=eateDepth;
			this.shallowScore[0]=shallowScore[0];
			this.shallowScore[1]=shallowScore[1];

		}
		public BitMaze clone() {
			BB newbit=this.bit.clone();
			//Snake[] newsnakes=snakes.clone();
			Snake[] newsnakes=new Snake[2];
			for(int i=0;i<2;i++) {
				newsnakes[i]=this.snakes[i].clone();
			}

			Coordinate newapple=null;
			if(this.apple!=null)newapple=new Coordinate(this.apple.x,this.apple.y);


			return new BitMaze(newbit,newsnakes,newapple,this.appleEater,this.hash,this.eateDepth,this.shallowScore);
		}
		public void move(int player,Direction d,int depth) {
			Coordinate head=snakes[player].body.getFirst();
			Coordinate tail=snakes[player].body.getLast();

			Coordinate newHead=head.moveTo(d);
			boolean grow=false;
			boolean zaoriku=false;//appleの復活フラグ
			if(player==1&&this.apple==null&&newHead.equals(this.snakes[(player+1)%2].getHead())) {
				this.apple=newHead;//同時に動いてるのに先手がアップルとったらなくなっちゃうの防ぐ
				zaoriku=true;
			}
			//myprintln("move newHead "+newHead.x+" "+newHead.y);

			if(newHead.equals(this.apple)) {
				//myprintln("move eater");
				this.apple=null;
				this.appleEater=player;
				grow=true;
				this.eateDepth=depth;
			}
			if(this.apple==null&& newHead.equals(snakes[(player+1)%2].getHead())) {
				this.appleEater=2;
				grow=true;
				this.eateDepth=depth;
			}

			if(!grow)this.bit.remove(tail);
			this.bit.set(newHead);
			this.snakes[player].moveTo(d, grow);


			if(player==0) {
				this.hash^=Hash.myBody[head.y][head.x];//もとのヘッドはボディ扱いに
				this.hash^=Hash.myHead[head.y][head.x];//もとのヘッドをhashのヘッドから抜く
				this.hash^=Hash.myHead[newHead.y][newHead.x];//新しいヘッドを追加
				if(!grow) {
					this.hash^=Hash.myBody[tail.y][tail.x];//もとのしっぽはボディから抜く
				}
			}else {
				this.hash^=Hash.oppBody[head.y][head.x];//もとのヘッドはボディ扱いに
				this.hash^=Hash.oppHead[head.y][head.x];//もとのヘッドをhashのヘッドから抜く
				this.hash^=Hash.oppHead[newHead.y][newHead.x];//新しいヘッドを追加
				if(!grow) {
					this.hash^=Hash.oppBody[tail.y][tail.x];//もとのしっぽはボディから抜く
				}
			}

			if(grow&&!zaoriku) {//復活なしでappleが消えた
				this.hash^=Hash.apple[newHead.y][newHead.x];//appleを消す
			}

		}
		public boolean canMove(int SorE,Direction act) {

			Coordinate nextHead=this.snakes[SorE].getHead().moveTo(act);
			if(!nextHead.inBounds(mazeSize)) { return false;}
			if(SorE==1&& nextHead.equals(this.snakes[(SorE+1)%2].getHead())){return true;}
			if(this.bit.isBit(nextHead)){ return false;}
			return true;

		}

		//ターン経過と共にしっぽを削る。playerはvirtualDirection方向に移動した場所にいると仮定して計算する
		//今いる場所が領域を分断している場合に面積を正しく計算できないため
		public RetVal getRemoveS(int player,Direction virtualDirection) {
			RetVal ret=new RetVal();
			//myprintln("getRemoveS player "+player+" isEater "+this.appleEater);
			if(this.appleEater==player||this.appleEater==2) {
				//myprintln("getRemoveS player "+player+" isEater");
				ret.appleDistance=-1;
				ret.orgAppleDistance=-1;
			}else {
				ret.appleDistance=14*14;
				ret.orgAppleDistance=14*14;
			}

			Snake[] tmpSnakes=new Snake[2];
			for(int i=0;i<2;i++) {
				tmpSnakes[i]=this.snakes[i].clone();
			}

			BB allboard=this.bit.clone();
			BB posbb=new BB();

			Coordinate posCo=this.snakes[player].getHead();
			if(virtualDirection!=null)posCo=posCo.moveTo(virtualDirection);//SnakeではなくCoordinateのmotveToを使うことで、Snakeを破壊しない
			posbb.set(posCo);

			boolean appleTouch=false;
			boolean orgAppleTouch=false;

			for(int cnt=0;;cnt++) {
				for(Snake tsnake:tmpSnakes) {
					if(!tsnake.body.isEmpty()) {
						Coordinate tail=tsnake.body.pollLast();
						//posbb.bit[tail.y]&=~(1<<tail.x);//ここallboardかもしれない
						allboard.bit[tail.y]&=~(1<<tail.x);
					}
				}
				//posbb.print();
				if(!appleTouch&&posbb.isBit(this.apple)) {ret.appleDistance=cnt;appleTouch=true;}
				if(!orgAppleTouch&& posbb.isBit(Aoki_eita1130.orgApple)) {ret.orgAppleDistance=cnt;orgAppleTouch=true;}
				boolean moved=false;
				BB nposbb=new BB();
				for (int y = 0; y < Aoki_eita1130.mazeSize.y; ++y ) {
					nposbb.bit[y]|=posbb.bit[y]<<1;
					nposbb.bit[y]|=posbb.bit[y]>>1;
					if(y>=1) {
						nposbb.bit[y]|=posbb.bit[y-1];
					}
					if(y<= Aoki_eita1130.mazeSize.y-2) {
						nposbb.bit[y]|=posbb.bit[y+1];
					}
					nposbb.bit[y]&=nposbb.filter;//filterは本当はstaticにしたい
					nposbb.bit[y]&=~allboard.bit[y];
					if((nposbb.bit[y]&~posbb.bit[y])!=0)moved=true;
				}
				if(!moved)break;
				for (int y = 0; y < Aoki_eita1130.mazeSize.y; ++y ) {
					posbb.bit[y]|=nposbb.bit[y];
				}

			}
			int retS=0;

			for (int y = 0; y < Aoki_eita1130.mazeSize.y; ++y ) {
				int tmpS=Long.bitCount( posbb.bit[y]);
				//myprintln("tmpS\t"+tmpS);
				retS+=tmpS;
			}
			ret.S=retS;
			//if(ret.appleDistance<0&&this.appleEater!=player) {ret.appleDistance=Thunder.mazeSize.x*Thunder.mazeSize.y;}
			//if(this.appleEater==player) {ret.appleDistance--;}
			//myprintln("retAppleDistance\t"+ret.appleDistance);
			return ret;
		}

		//二人の動きを考慮した面積などを計算する
		public RetVal[] getWS(int kijun_player,Direction virtualDirection) {
			//myprintln("getWS");
			RetVal[] rets= {new RetVal(),new RetVal()};
			//myprintln("getRemoveS player "+player+" isEater "+this.appleEater);
			for(int player=0;player<2;player++) {
			if(this.appleEater==player||this.appleEater==2) {
				//myprintln("getRemoveS player "+player+" isEater");
				rets[player].appleDistance=-1;
				rets[player].orgAppleDistance=-1;
			}else {
				rets[player].appleDistance=14*14;
				rets[player].orgAppleDistance=14*14;
			}
			}
			Snake[] tmpSnakes=new Snake[2];
			for(int i=0;i<2;i++) {
				tmpSnakes[i]=this.snakes[i].clone();
			}

			BB allboard=this.bit.clone();
			BB[] posbbs= {new BB(),new BB()};

			int notKijun=(kijun_player+1)%2;

			Coordinate[] posCo= {this.snakes[0].getHead(),this.snakes[1].getHead()};

			if(virtualDirection!=null)posCo[kijun_player]=posCo[kijun_player].moveTo(virtualDirection);//SnakeではなくCoordinateのmotveToを使うことで、Snakeを破壊しない

			for(int i=0;i<2;i++) {
				posbbs[i].set(posCo[i]);
			}


			boolean[] appleTouch= {false,false};

			for(int cnt=0;;cnt++) {
				for(Snake tsnake:tmpSnakes) {
					if(!tsnake.body.isEmpty()) {
						Coordinate tail=tsnake.body.pollLast();
						allboard.bit[tail.y]&=~(1<<tail.x);
					}
				}
				//posbb.print();
				int notmoveCnt=0;
				for(int pi=1;pi<=2;pi++) {
					int nowPlayer=(kijun_player+pi)%2;
					if(!appleTouch[nowPlayer]&&posbbs[nowPlayer].isBit(this.apple)) {rets[nowPlayer].appleDistance=cnt;appleTouch[nowPlayer]=true;}
					boolean moved=false;
					BB nposbb=new BB();
					for (int y = 0; y < Aoki_eita1130.mazeSize.y; ++y ) {
						nposbb.bit[y]|=posbbs[nowPlayer].bit[y]<<1;
						nposbb.bit[y]|=posbbs[nowPlayer].bit[y]>>1;
						if(y>=1) {
							nposbb.bit[y]|=posbbs[nowPlayer].bit[y-1];
						}
						if(y<= Aoki_eita1130.mazeSize.y-2) {
							nposbb.bit[y]|=posbbs[nowPlayer].bit[y+1];
						}
						nposbb.bit[y]&=nposbb.filter;//filterは本当はstaticにしたい
						nposbb.bit[y]&=~allboard.bit[y];
						if((nposbb.bit[y]&~posbbs[nowPlayer].bit[y])!=0)moved=true;
					}
					if(!moved) {notmoveCnt++;continue;}
					for (int y = 0; y < Aoki_eita1130.mazeSize.y; ++y ) {
						posbbs[nowPlayer].bit[y]|=nposbb.bit[y];
						allboard.bit[y]|=nposbb.bit[y];
					}
				}
				if(notmoveCnt==2)break;

			}


			for(int i=0;i<2;i++) {
				int retS=0;
			for (int y = 0; y < Aoki_eita1130.mazeSize.y; ++y ) {
				int tmpS=Long.bitCount( posbbs[i].bit[y]);
				retS+=tmpS;
			}
			rets[i].S=retS;
			}
			//if(ret.appleDistance<0&&this.appleEater!=player) {ret.appleDistance=Thunder.mazeSize.x*Thunder.mazeSize.y;}
			//if(this.appleEater==player) {ret.appleDistance--;}
			//myprintln("retAppleDistance\t"+ret.appleDistance);
			return rets;
		}


		public boolean isShoutotsu() {
			//myprintln("isShoutosu function");
			Coordinate my=this.snakes[0].getHead();
			Coordinate opp=this.snakes[1].getHead();
			//myprintln("my "+my.x+" , "+my.y);
			//myprintln("opp "+opp.x+" , "+opp.y);

			return this.snakes[0].getHead().equals(this.snakes[1].getHead());
		}

		public void print() {print(true);}
		public void print(boolean createCode) {
			for(int x=0;x<mazeSize.x;x++) {

				myprint('-');
			}

			myprintln("");
			myprint("//\t");
			for(int x=0;x<mazeSize.x;x++) {

				myprint(x%10);
			}

			myprintln();
			for(int y=0;y<mazeSize.y;y++) {
				myprint("//\t");
				for(int x=0;x<mazeSize.x;x++) {

					long val=(bit.bit[y]>>x)&1L;
					char ch=val==0?'.':'#';
					if(this.snakes[1].elements.contains(new Coordinate(x,y)))ch=val==0?'.':'*';
					if(eqCoord(this.apple,x,y)) {ch='@';}
					for(int i=0;i<2;i++) {
						if(eqCoord(this.snakes[i].getHead(),x,y)) {ch=(char) ((int)'0'+i);}
					}
					myprint(ch);
				}

				myprintln(y);
			}


			if(createCode){
				int snakei=0;
				for(Snake snake:this.snakes) {
					String name=(snakei==0)?"my":"opp";
					ArrayList<Coordinate>revbody=new ArrayList<Coordinate>();revbody.addAll(snake.body);
					//Collections.reverse(revbody);
					Coordinate tail=revbody.get(0);
					revbody.remove(0);
					myprint("Snake "+name+" =new Snake( new Coordinate("+tail.x+","+tail.y   +"),mazeSize);");
					for(Coordinate co:revbody) {
						myprint(name+".body.add(new Coordinate("+co.x+","+co.y   +"));");
						myprint(name+".elements.add(new Coordinate("+co.x+","+co.y   +"));");
					}
					snakei++;
				}
				myprintln("Coordinate apple=new Coordinate("+apple.x+","+apple.y   +");");
			}
		}
	}


	private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

	public static long getPerScoreAppleFast(BitMaze tmpMaze,RetVal info) {
		long score=0;
		score-=info.appleDistance;
		score*=200;
		score+=info.S;
		return score;
	}

	public static long getPerScoreSquareFast(BitMaze tmpMaze,RetVal info) {
		long score=0;

		score+=info.S;

		score*=200;
		score-=info.appleDistance;

		return score;
	}
	public static long getPerScore(BitMaze tmpMaze,RetVal info,Func func) {
	switch(func) {
	case AppleFast:
		return getPerScoreAppleFast(tmpMaze,info);
	case SquareFast:
		return getPerScoreSquareFast(tmpMaze,info);
	}
	return Long.MIN_VALUE;
	}
enum Func{
	AppleFast,
	SquareFast,
	}

	public static RetVal getBestInfo(BitMaze maze,int player) {
		RetVal bestInfo=null;

		long bestScore=Long.MIN_VALUE;
		for(Direction d:Direction.values()) {
			if(!maze.canMove(player, d)) {continue;}
			RetVal tmpInfo=maze.getRemoveS(player, d);
			long tmpScore=getPerScoreAppleFast(maze,tmpInfo);
			if(tmpScore>bestScore) {bestScore=tmpScore;bestInfo=tmpInfo;}
		}
		if(bestInfo==null) {
			bestInfo=maze.getRemoveS(player, null);
		}
		return bestInfo;
	}

	public static RetVal[] getBestWInfos(BitMaze maze,int player,Func func) {
RetVal[]bestInfos=null;
		long bestScore=Long.MIN_VALUE;
		for(Direction d:Direction.values()) {
			//myprintln("d "+d);
			if(!maze.canMove(player, d)) {continue;}
			//myprintln("cand "+d);
			RetVal[] tmpInfos=maze.getWS(player, d);
			long tmpScore=getPerScore(maze,tmpInfos[player],func)-getPerScore(maze,tmpInfos[(player+1)%2],func);
			if(tmpScore>bestScore) {bestScore=tmpScore;bestInfos=tmpInfos;}
		}
		if(bestScore==Long.MIN_VALUE) {
			RetVal[] tmpInfos=maze.getWS(player, null);
			long tmpScore=getPerScore(maze,tmpInfos[player],func)-getPerScore(maze,tmpInfos[(player+1)%2],func);
			if(tmpScore>bestScore) {bestScore=tmpScore;bestInfos=tmpInfos;}
		}
		//myprintln("getBestWScore end");
		return bestInfos;
	}
	public static boolean hameWin(BitMaze orgMaze,int player) {
		int oppId=(player+1)%2;

		Coordinate mySnakeHead=orgMaze.snakes[player].getHead();
		Coordinate oppSnakeHead=orgMaze.snakes[oppId].getHead();
		if(mySnakeHead.x==1&&oppSnakeHead.x==0) {
			int step=(mySnakeHead.y>oppSnakeHead.y)?1:-1;
			for(int i=0;i<Math.abs(mySnakeHead.y-oppSnakeHead.y)+1;i++) {
				int oy=oppSnakeHead.y+i*step;
				//System.err.println("oy "+oy);
				if(!orgMaze.bit.isBit(mySnakeHead.x,oy))return false;
			}
			return true;
		}
		if(mySnakeHead.x== Aoki_eita1130.mazeSize.x- 2&&oppSnakeHead.x== Aoki_eita1130.mazeSize.x- 1) {
			int step=(mySnakeHead.y>oppSnakeHead.y)?1:-1;
			for(int i=0;i<Math.abs(mySnakeHead.y-oppSnakeHead.y)+1;i++) {
				int oy=oppSnakeHead.y+i*step;
				if(!orgMaze.bit.isBit(mySnakeHead.x,oy))return false;
			}
			return true;
		}
		if(mySnakeHead.y==1&&oppSnakeHead.y==0) {
			int step=(mySnakeHead.x>oppSnakeHead.x)?1:-1;
			for(int i=0;i<Math.abs(mySnakeHead.x-oppSnakeHead.x)+1;i++) {
				int ox=oppSnakeHead.x+i*step;
				if(!orgMaze.bit.isBit(ox,mySnakeHead.y))return false;
			}
			return true;
		}
		if(mySnakeHead.y== Aoki_eita1130.mazeSize.y-2&&oppSnakeHead.y== Aoki_eita1130.mazeSize.y-1) {
			int step=(mySnakeHead.x>oppSnakeHead.x)?1:-1;
			for(int i=0;i<Math.abs(mySnakeHead.x-oppSnakeHead.x)+1;i++) {
				int ox=oppSnakeHead.x+i*step;
				if(!orgMaze.bit.isBit(ox,mySnakeHead.y))return false;
			}
			return true;
		}
		return false;
	}
	public static long getScore(BitMaze orgMaze,int player) {
		BitMaze tmpMaze=orgMaze.clone();

		int oppId=(player+1)%2;


		long score=0;




		//		RetVal myInfo=tmpMaze.getRemoveS(player);
		//		RetVal oppInfo=tmpMaze.getRemoveS(oppId);

//		if(superWinner==player) {
//			RetVal[] infos=getBestWInfos(tmpMaze,player,Func.SquareFast);
//			return getPerScore(tmpMaze,infos[player],Func.SquareFast)-getPerScore(tmpMaze,infos[oppId],Func.SquareFast);
//
//		}
		RetVal myInfo=null;
		RetVal oppInfo=null;
		boolean isJoban=tmpMaze.snakes[0].body.size()+tmpMaze.snakes[1].body.size()<=20;
		if(isJoban) {//序盤
//			myInfo=getBestInfo(tmpMaze,player);//面積雑にしようとしたけど端でおいこむやつに負けるのでダメ
//			oppInfo=getBestInfo(tmpMaze,oppId);
			RetVal[] infos=getBestWInfos(tmpMaze,player,Func.SquareFast);

			myInfo=infos[player];
			oppInfo=infos[oppId];
		}else//後半
		{
			RetVal[] infos=getBestWInfos(tmpMaze,player,Func.SquareFast);

			myInfo=infos[player];
			oppInfo=infos[oppId];
		}

		{
			if(myInfo.S>oppInfo.S) {//面積勝ち状態
				score=1;
				if(oppInfo.S<tmpMaze.snakes[oppId].body.size()) {
					score*=2;
				}
				else if(true) {score*=0;}//序盤はぐるぐるならないようにする
			}
			if(myInfo.S<oppInfo.S) {//面積負け状態
				score=-1;

				if(myInfo.S<tmpMaze.snakes[player].body.size()) {
					score*=2;
				}
				else if(true) {score*=0;}//序盤はぐるぐるならないようにする
			}
		}
		if(Aoki_eita1130.hameWin(tmpMaze, player))score+=1;
		if(Aoki_eita1130.hameWin(tmpMaze, oppId))score-=1;

		score*=200;
		int tmpEateDepth=100;

		if(isJoban)if(tmpMaze.appleEater==2||tmpMaze.appleEater==player)tmpEateDepth=tmpMaze.eateDepth;
		score-=tmpEateDepth;;

		score*=200*200*200;
		score+=getPerScoreAppleFast(tmpMaze,myInfo)-getPerScoreAppleFast(tmpMaze,oppInfo);

		if(tmpMaze.shallowScore[player]!=null) {
			//myprintln("aaaaaaaaaa");
			score*=400;
			//score*=0;

			score+=tmpMaze.shallowScore[player];
		}

		return score;
	}
	public static long getShallowScore(BitMaze orgMaze,int player) {
		BitMaze tmpMaze=orgMaze.clone();

		int oppId=(player+1)%2;


		long score=0;

		RetVal myInfo=tmpMaze.getRemoveS(player,null);
		RetVal oppInfo=tmpMaze.getRemoveS(oppId,null);

		score=-myInfo.appleDistance+oppInfo.appleDistance;

		return score;
	}


	private static ArrayList<Direction> MakeMoveFirst(int SorE,int depth,BitMaze k) {
		ArrayList<Direction> teBuf=new ArrayList<Direction>();
		if (!HashTbl.containsKey(k.hash)) {
			return teBuf;
		}
		HashEntry hashEntry=HashTbl.get(k.hash);

		if (hashEntry.depth%2!=depth%2) {
			// 手番が違う。
			return teBuf;
		}

		// 局面が一致したと思われる
		Direction te=hashEntry.Best;
		if (te!=null) {
			if (k.canMove(SorE,te)) {
				teBuf.add(te);
			}
		}
		te=hashEntry.Second;
		if (te!=null) {
			if (k.canMove(SorE,te)) {
				teBuf.add(te);
			}
		}
		if (depth>1) {
			te=Best[depth-1][depth];
			if (te!=null) {
				if (k.canMove(SorE,te)) {
					teBuf.add(te);
				}
			}
		}
		return teBuf;
	}

	public static long INF=Long.MAX_VALUE/10;
	public static Direction[][] Best;

	public static void hashAdd(BitMaze k,int depth,long retval,long alpha,long beta) {

		HashEntry e=new HashEntry();
		if (HashTbl.containsKey(k.hash)) {
			e=HashTbl.get(k.hash);
			e.Second=e.Best;
		} else {
			if (e.depth == depth) {
				// ハッシュに残っている値の方が重要なので、登録をあきらめる
				return;
			}

			e.Second=Direction.DOWN;
		}
		if (retval>alpha) {
			e.Best=Best[depth][depth];
		} else {
			e.Best=Direction.DOWN;
		}
		e.value=retval;
		if (retval<=alpha) {
			e.flag=FLAG.UPPER_BOUND;
		} else if (retval>=beta) {
			e.flag=FLAG.LOWER_BOUND;
		} else {
			e.flag=FLAG.EXACTLY_VALUE;
		}
		e.depth=depth;
		//e.remainDepth=depthMax-depth;
		//e.Tesu=k.Tesu;
		HashTbl.put(k.hash, e);
	}

	static boolean akirame=false;
	public static long NegaAlphaBeta(int SorE, BitMaze k, long  alpha, long beta, int depth, int depthMax) {
		if(akirame||System.currentTimeMillis()- Aoki_eita1130.startTime>threTime) {akirame=true;return -INF;}


		if(k.isShoutotsu()&&depth%2==0){
			//myprintln("shototu");
			boolean loose=(k.snakes[SorE].body.size()<k.snakes[(SorE+1)%2].body.size());
			if(loose) {
				//myprintln("SorE "+SorE+"\tLOOZE");
				return -INF/2;
			}
		}
		if(k.isShoutotsu()&&depth%2==1){
			//myprintln("shototu");
			boolean win=(k.snakes[SorE].body.size()>k.snakes[(SorE+1)%2].body.size());
			if(win) {
				//myprintln("SorE "+SorE+"\tLOOZE");
				return INF/2;
			}
		}

		if(depth==depthMax) {
			long score= Aoki_eita1130.getScore(k,SorE);
			//myprintln("nega\t"+score);
			return score;
		}

		if(depth==2) {
			k.shallowScore[0]= Aoki_eita1130.getShallowScore(k, 0);
			k.shallowScore[1]=-k.shallowScore[0];
		}

		if (HashTbl.containsKey(k.hash)) {
			HashEntry e=HashTbl.get(k.hash);
			if(e.value>=beta&&e.depth>=depth&&e.depth%2==depth&&e.flag!=FLAG.UPPER_BOUND) {
				return e.value;
			}
			//			if(e.value<=alpha&&e.Tesu>=depth&&e.tes) {
			//				return e.value;
			//			}

		}

		long retval = -INF-1;
		int teNum = 0;//行動できるかチェック
//
//		for (Direction act:Thunder.MakeMoveFirst(SorE, depthMax, k)) {
//			BitMaze kk=k.clone();
//			if (!kk.canMove(SorE, act))continue;
//			teNum++;
//			kk.move(SorE, act);
//			long v = -NegaAlphaBeta((SorE+1)%2, kk, -beta, -alpha, depth + 1, depthMax);
//			//myprintln("v "+v);
//			if (v>retval) {
//				retval = v;
//				Best[depth][depth] = act;
//				for (int i = depth + 1; i<depthMax; i++) {
//					Best[depth][i] = Best[depth + 1][i];
//				}
//				if (retval>alpha) {
//					alpha = retval;
//				}
//				if (retval >= beta) {
//					Thunder.hashAdd(k, depthMax, retval, alpha, beta);
//					return retval;
//				}
//			}
//
//		}

		for (Direction act:Direction.values()) {
			BitMaze kk=k.clone();

			if (!kk.canMove(SorE, act))continue;

			teNum++;
			kk.move(SorE, act,depth);
			long v = -NegaAlphaBeta((SorE+1)%2, kk, -beta, -alpha, depth + 1, depthMax);
			//myprintln("v "+v);
			if (v>retval) {
				retval = v;
				Best[depth][depth] = act;
				for (int i = depth + 1; i<depthMax; i++) {
					Best[depth][i] = Best[depth + 1][i];
				}
				if (retval>alpha) {
					alpha = retval;
				}
				if (retval >= beta) {
					Aoki_eita1130.hashAdd(k, depthMax, retval, alpha, beta);
					return retval;
				}
			}
		}
		if (teNum == 0) {
			// 負け
			return -INF;
		}


			return retval;
	}
	static  int BSIZE=0;
	//27 LEFT
	//15 LEFT
	//14 RIGHT
	public static Direction NegaAlphaBeta(BitMaze k, int depthMax)
	{

		NegaAlphaBeta(0, k, -Long.MAX_VALUE, Long.MAX_VALUE, 0, depthMax);
		return Best[0][0];
	}

	static long startTime;
	static long threTime=500;//500;
	public static Direction ITDeep(BitMaze k) {
		int firstSIZE=10;//////////////////ここ重要。BISIZE-1より小さくしないとダメ
		if(Aoki_eita1130.superWinner==0) {
			BSIZE=60+1;
		}else {
			BSIZE=60+1;
			//BSIZE=12+1;
		}


		Aoki_eita1130.Best=new Direction[BSIZE][BSIZE];
		for (int i = 0; i < Aoki_eita1130.BSIZE; i++) {
			for (int j = 0; j < Aoki_eita1130.BSIZE; j++) {
				Aoki_eita1130.Best[i][j] = null;
			}
		}

		Direction dir=null;
		int searchedDepth=0;
		int maxDepth=BSIZE;
//		if(k.snakes[0].body.size()<=k.snakes[1].body.size()&&k.snakes[0].body.size()<14) {
//			maxDepth=7;
//		}

		//for(int i=1;i<4;i++) {
		//for(int i=1;i<maxDepth;i++) {
			for(int i=firstSIZE;i<maxDepth;i+=2) {
					Direction tmpDir=NegaAlphaBeta(k,i);
			if(akirame)break;
			//if(i%2==1)
			{dir=tmpDir;}
			searchedDepth=i;
			if(System.currentTimeMillis()- Aoki_eita1130.startTime>threTime) {

				break;
			}
		}
		myprintln("depth\t"+searchedDepth);
		return dir;
	}

	/**
	 * Choose the direction (not rational - silly)
	 * @param snake    Your snake's body with coordinates for each segment
	 * @param opponent Opponent snake's body with coordinates for each segment
	 * @param mazeSize Size of the board
	 * @param apple    Coordinate of an apple
	 * @return Direction of bot's move
	 */
	@Override
	public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
		Aoki_eita1130.initTurn(apple);

		BitMaze nowMaze=new BitMaze(snake,opponent,apple);
		nowMaze.print();

		superWinner=-1;
		if(nowMaze.snakes[0].body.size()-nowMaze.snakes[1].body.size()>=2)superWinner=0;
		if(nowMaze.snakes[1].body.size()-nowMaze.snakes[0].body.size()>=2)superWinner=1;

		Direction bestDir= Aoki_eita1130.ITDeep(nowMaze.clone());
		//Direction bestDir=Thunder.NegaAlphaBeta(nowMaze.clone(),14);

		if(bestDir==null) {
			myprintln("bestDir==null");
			bestDir=Direction.DOWN;

			for (Direction act:Direction.values()) {
				BitMaze kk=nowMaze.clone();

				if (kk.canMove(0, act)) {bestDir=act;break;}

			}

		}
		return bestDir;
		//        if (notLosing.length > 0) return notLosing[0];
		//        else return validMoves[0];
		/* Cannot avoid losing here */
	}

	public static void main(String[] args)
	{


		myprintln("thunder debug main");
//		01234567890123
//		..............0
//		..............1
//		..............2
//		@........**...3
//		........**....4
//		......***.....5
//		....***.......6
//		.1***.........7
//		.###..........8
//		.#.#..........9
//		.#.##.........10
//		.#.##.........11
//		.#.##.........12
//		.#0...........13
	//Snake my =new Snake( new Coordinate(2,13),mazeSize);my.body.add(new Coordinate(1,13));my.elements.add(new Coordinate(1,13));my.body.add(new Coordinate(1,12));my.elements.add(new Coordinate(1,12));my.body.add(new Coordinate(1,11));my.elements.add(new Coordinate(1,11));my.body.add(new Coordinate(1,10));my.elements.add(new Coordinate(1,10));my.body.add(new Coordinate(1,9));my.elements.add(new Coordinate(1,9));my.body.add(new Coordinate(1,8));my.elements.add(new Coordinate(1,8));my.body.add(new Coordinate(2,8));my.elements.add(new Coordinate(2,8));my.body.add(new Coordinate(3,8));my.elements.add(new Coordinate(3,8));my.body.add(new Coordinate(3,9));my.elements.add(new Coordinate(3,9));my.body.add(new Coordinate(3,10));my.elements.add(new Coordinate(3,10));my.body.add(new Coordinate(3,11));my.elements.add(new Coordinate(3,11));my.body.add(new Coordinate(3,12));my.elements.add(new Coordinate(3,12));my.body.add(new Coordinate(4,12));my.elements.add(new Coordinate(4,12));my.body.add(new Coordinate(4,11));my.elements.add(new Coordinate(4,11));my.body.add(new Coordinate(4,10));my.elements.add(new Coordinate(4,10));Snake opp =new Snake( new Coordinate(1,7),mazeSize);opp.body.add(new Coordinate(2,7));opp.elements.add(new Coordinate(2,7));opp.body.add(new Coordinate(3,7));opp.elements.add(new Coordinate(3,7));opp.body.add(new Coordinate(4,7));opp.elements.add(new Coordinate(4,7));opp.body.add(new Coordinate(4,6));opp.elements.add(new Coordinate(4,6));opp.body.add(new Coordinate(5,6));opp.elements.add(new Coordinate(5,6));opp.body.add(new Coordinate(6,6));opp.elements.add(new Coordinate(6,6));opp.body.add(new Coordinate(6,5));opp.elements.add(new Coordinate(6,5));opp.body.add(new Coordinate(7,5));opp.elements.add(new Coordinate(7,5));opp.body.add(new Coordinate(8,5));opp.elements.add(new Coordinate(8,5));opp.body.add(new Coordinate(8,4));opp.elements.add(new Coordinate(8,4));opp.body.add(new Coordinate(9,4));opp.elements.add(new Coordinate(9,4));opp.body.add(new Coordinate(9,3));opp.elements.add(new Coordinate(9,3));opp.body.add(new Coordinate(10,3));opp.elements.add(new Coordinate(10,3));Coordinate apple=new Coordinate(0,3);
//my.moveTo(Direction.RIGHT, false);
//my.moveTo(Direction.LEFT, false);

		Coordinate apple=new Coordinate(7,7);
		Snake my=new Snake( new Coordinate(4,12),mazeSize);for(int i=0;i<2;i++)my.moveTo(Direction.RIGHT, true);
		Snake opp=new Snake( new Coordinate(5,13),mazeSize);


Aoki_eita1130 aokieita1130gmailcom =new Aoki_eita1130();
		aokieita1130gmailcom.initTurn(apple);
BitMaze bmazeorg= aokieita1130gmailcom.new BitMaze(my.clone(),opp.clone(),new Coordinate(apple.x,apple.y));
bmazeorg.print();

System.err.println("isHame "+ Aoki_eita1130.hameWin(bmazeorg, 0));
if(true) {return;}

Direction bestd= aokieita1130gmailcom.chooseDirection(bmazeorg.snakes[0], bmazeorg.snakes[1], mazeSize, bmazeorg.apple);

myprintln("best d\t"+bestd);

myprintln("Direction[][] myDirections=new Direction[][] {{");
for(int i=0;i<Best.length;i+=2) {
	if(Best[0][i]==null)break;
	myprintln("Direction."+Best[0][i]+",");

}
myprintln("}};");
myprintln("Direction[][] oppDirections=new Direction[][]{{");
for(int i=1;i<Best.length;i+=2) {
	if(Best[0][i]==null)break;
	myprintln("Direction."+Best[0][i]+",");
}
myprintln("}};");


Direction[][] myDirections=new Direction[][] {
	{Direction.DOWN,},
{Direction.RIGHT,},


};
Direction[][] oppDirections=new Direction[][]{
	{Direction.DOWN,},
	{Direction.DOWN,},
};
for(int i=0;i<myDirections.length;i++){
	Direction[] myds=myDirections[i];
	Direction[] oppds=oppDirections[i];
	aokieita1130gmailcom =new Aoki_eita1130();
	aokieita1130gmailcom.initTurn(apple);

	BitMaze bmaze= aokieita1130gmailcom.new BitMaze(my.clone(),opp.clone(),new Coordinate(apple.x,apple.y));
	for(int j=0;j<oppds.length;j++) {
		bmaze.move(0,myds[j] ,j);
		bmaze.move(1,oppds[j],j );
	}

//	RetVal myinfo=bmaze.getRemoveS(0);
//	RetVal oppinfo=bmaze.getRemoveS(1);
	RetVal myinfo=getBestInfo(bmaze,0);
	RetVal oppinfo=getBestInfo(bmaze,1);

	bmaze.print(false);
	for(Direction d:myds) {myprint(d+" ");}myprintln();
	myprintln("myS\t"+myinfo.S);
	myprintln("appleDistance\t"+myinfo.appleDistance);
	myprintln("orgAppleDistance\t"+myinfo.orgAppleDistance);
	myprintln("myPreScore\t"+ Aoki_eita1130.getPerScoreAppleFast(bmaze, myinfo));
	myprintln("oppPreScore\t"+ Aoki_eita1130.getPerScoreAppleFast(bmaze, oppinfo));
	//myprintln("myScore\t"+Thunder.getScore(bmaze, 0));
	//myprintln("oppScore\t"+Thunder.getScore(bmaze, 1));
	myprintln();
	}




}

//for(Direction d:new Direction[] {Direction.LEFT,Direction.RIGHT}){
//	for(Direction d:new Direction[] {Direction.LEFT,Direction.RIGHT,Direction.DOWN}){
//
//
//		thunder=new Thunder();thunder.initTurn(apple);
//
//		BitMaze bmaze=thunder.new BitMaze(my.clone(),opp.clone(),new Coordinate(apple.x,apple.y));
//		bmaze.move(0, d);
//		bmaze.move(1, Direction.DOWN);
//
//		RetVal myinfo=bmaze.getRemoveS(0);
//
//		myprintln(d);
//		myprintln("myS\t"+myinfo.S);
//		myprintln("appleDistance\t"+myinfo.appleDistance);
//		myprintln("orgAppleDistance\t"+myinfo.orgAppleDistance);
//		myprintln("myPreScore\t"+Thunder.getPerScore(bmaze, myinfo));
//		myprintln("myScore\t"+Thunder.getScore(bmaze, 0));
//		myprintln();
//		}
//
//
//
//
//	}
}