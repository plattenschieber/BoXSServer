package server

import groovy.lang.Closure

import java.util.ArrayList

class MirkoGroovyLibrary {
	static execute() {
		
		ArrayList.metaClass {
			mean={ delegate.sum()/delegate.size() }
			median={
				if (delegate.size()%2==1)
					delegate.getAt((int)(delegate.size()/2))
				else
					(delegate.getAt((int)(delegate.size()/2-0.5))+delegate.getAt((int)(delegate.size()/2+0.5)))/2
			}
			getRandomElement={delegate[new Random().nextInt(delegate.size())]}
		}
		
		LinkedList.metaClass
		{
			getRandomElement={delegate[new Random().nextInt(delegate.size())]}
		}


		double[].metaClass {
			getSum={def s=0; delegate.each{s+=it}; s}
		}

		double[][].metaClass{
			getRows {delegate.size()}
			getCols {delegate[0].size()}
			getSum {def s=0; delegate.each{s+=it.sum}; s}
			getTotalsize {def s=0; delegate.each{s+=it.size()}; s}
			getMean{delegate.sum / delegate.totalsize}

			applyToEach={ 
				for (int row:0..<delegate.rows)
					for (int col:0..<delegate.cols)
						delegate[row][col]=it.call(row,col)
				delegate
			}

			getTranspose= {
				new double[delegate.cols][delegate.rows].applyToEach { row, col -> delegate[col][row]}
			}


			copy= {
				new double[delegate.rows][delegate.cols].applyToEach { row, col -> delegate[row][col]}
			}

			multiply << { double d -> delegate.copy().applyToEach { it * d  }}
			multiply << { double[][] d -> 
								new double[delegate.rows][d.cols].applyToEach { row, col ->
								(0..<d.rows).sum {i->delegate[row][i] * d[i][col] 	} }	}
/*
			multiply={
				if (it instanceof Double || it instanceof BigDecimal || it instanceof Integer)
					delegate.copy()
				else if (it instanceof double[][])
					new double[delegate.rows][it.cols].applyToEach { row, col ->
						(0..<it.rows).sum {i->delegate[row][i] * it[i][col]
						} }
			}
*/

			// Elementwise substraction
			mod={
				new double[delegate.rows][delegate.cols].applyToEach { row, col ->
					delegate[row][col] - it[row][col]}
			}

			identityMatrix={ l->
				double[][] ret=new double[l][l]; (0..<l).each {ret[it][it]=1} ; ret
			}

			getDiagonal={
				(0..<delegate.rows).collect{delegate[it][it]}
			}

			getInverse={
				double[][] a=delegate.copy{it}, i=delegate.identityMatrix(delegate.rows)

				// Hauptdiagonale
				for (int elem:0..<delegate.rows)
				{
					// eigene Zeile normieren
					double factor=1/a[elem][elem];
					for (int col:0..<delegate.rows)
					{
						a[elem][col]=a[elem][col]*factor
						i[elem][col]=i[elem][col]*factor
					}

					// abziehen
					for (int row:0..<delegate.rows)
					{
						if (row==elem) continue;
						factor=a[row][elem];
						for (int col:0..<delegate.rows)
						{
							a[row][col]=a[row][col]-factor*a[elem][col];
							i[row][col]=i[row][col]-factor*i[elem][col];
						}
					}
				}
				i
			}
		}

	}

	public static long time(Closure c)
	{
		long starttime=System.nanoTime()
		c.call()
		long endtime=System.nanoTime()
		(endtime-starttime)/1e6
	}


	static final double h=0.00001

	public static double derive(Closure c, double x)
	{
		(c.call(x+h)-c.call(x-h))/2/h
	}

	public static double integrate(Closure c, double start, double end)
	{
		(0..100).sum { c.call(start+(end-start)*it/100)/101 }
	}

	public static double[][] identity(int l)
	{
		double[][] ret=new double[l][l]; (0..<l).each {ret[it][it]=1}
	}

	public static scheduleAtFixedRate(int start, int rate, Closure c)
	{
		Thread.start {
			sleep(start)
			while (true)
			{
				c.call()
				sleep(rate)
			}
		}
	}

	public static scheduleAt(int start, Closure c)
	{
		Thread.start {
			sleep(start)
			c.call()
		}
	}

}
