package org.liboping;

/**
 * Created by n500579 on 5/19/16.
 */
public interface OpingObserver
{
	public void observe(String address, int i, int dropped, double timeout, double latency);
}
