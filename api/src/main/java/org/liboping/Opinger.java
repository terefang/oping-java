package org.liboping;

import com.sun.jna.*;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by n500579 on 5/19/16.
 */
public class Opinger implements OpingObserver
{
	static
	{
		try
		{
			File libfile = Native.extractFromResourcePath("/org/liboping/"+Platform.RESOURCE_PREFIX+"/liboping.so", Opinger.class.getClassLoader());
			Native.register(OpingLibrary.class, NativeLibrary.getInstance(libfile.getAbsolutePath()));
		}
		catch(Exception xe)
		{
			xe.printStackTrace(System.err);
		}
	}

	int retries = 3;
	double timeout = -1;
	int ttl = 64;
	int af = 4;

	String data = null;
	String source = null;
	String device = null;

	Set<String> addresses = new HashSet();
	Set<OpingObserver> observer = new HashSet();

	private PointerByReference opingObject;

	public void close()
	{
		OpingLibrary.ping_destroy(opingObject);
	}

	public void init()
	{
		Pointer m = null;

		opingObject = OpingLibrary.ping_construct();
		if(this.af>=0)
		{
			m = new Memory(4);
			m.setInt(0, this.af);
			OpingLibrary.ping_setopt(opingObject, OpingLibrary.PING_OPT_AF, m);
		}
		if(this.ttl>=0)
		{
			m = new Memory(4);
			m.setInt(0, this.ttl);
			OpingLibrary.ping_setopt(opingObject, OpingLibrary.PING_OPT_TTL, m);
		}
		if(this.timeout>=0)
		{
			m = new Memory(16);
			m.setDouble(0, this.timeout);
			OpingLibrary.ping_setopt(opingObject, OpingLibrary.PING_OPT_TIMEOUT, m);
		}
		if(this.data!=null)
		{
			m = new Memory(this.data.length() + 1);
			m.setString(0, this.data);
			OpingLibrary.ping_setopt(opingObject, OpingLibrary.PING_OPT_DATA, m);
		}
		if(this.source!=null)
		{
			m = new Memory(this.source.length() + 1);
			m.setString(0, this.source);
			OpingLibrary.ping_setopt(opingObject, OpingLibrary.PING_OPT_SOURCE, m);
		}
		if(this.device!=null)
		{
			m = new Memory(this.device.length() + 1);
			m.setString(0, this.device);
			OpingLibrary.ping_setopt(opingObject, OpingLibrary.PING_OPT_DEVICE, m);
		}

		for(String n : this.addresses)
		{
			OpingLibrary.ping_host_add(opingObject, n);
		}
	}

	public void addAddress(String address)
	{
		if(!this.addresses.contains(address))
		{
			OpingLibrary.ping_host_add(opingObject, address);
			this.addresses.add(address);
		}
	}

	public void removeAddress(String address)
	{
		if(this.addresses.contains(address))
		{
			OpingLibrary.ping_host_remove(opingObject, address);
			this.addresses.remove(address);
		}
	}

	public void runPing()
	{
		long t1, t2, t3 = 0;
		for(int i=0; i< this.retries; i++)
		{
			OpingLibrary.ping_send(opingObject);
		}
		OpingLibrary.ping_send(opingObject);
	}

	public void runPingOnce()
	{
			OpingLibrary.ping_send(opingObject);
	}

	public void runPost()
	{
		Pointer m = new Memory(128);
		NativeLong nl = new NativeLong(128L);
		NativeLongByReference ml = new NativeLongByReference(nl);

		PointerByReference iter = OpingLibrary.ping_iterator_get(opingObject);
		while(iter!=null)
		{
			OpingLibrary.ping_iterator_get_info(iter, OpingLibrary.PING_INFO_USERNAME, m, ml);
			String address = m.getString(0);
			nl.setValue(128L);
			ml.setValue(nl);
			OpingLibrary.ping_iterator_get_info(iter, OpingLibrary.PING_INFO_DROPPED, m, ml);
			int dropped = m.getInt(0);
			nl.setValue(128L);
			ml.setValue(nl);
			OpingLibrary.ping_iterator_get_info(iter, OpingLibrary.PING_INFO_LATENCY, m, ml);
			double latency = m.getDouble(0);
			nl.setValue(128L);
			ml.setValue(nl);
			for(OpingObserver ob : this.observer)
			{
				ob.observe(address, this.retries + 1, dropped, this.timeout, latency);
			}
			iter = OpingLibrary.ping_iterator_next(iter);
		}
	}

	public void runPostOnce()
	{
		Pointer m = new Memory(128);
		NativeLong nl = new NativeLong(128L);
		NativeLongByReference ml = new NativeLongByReference(nl);

		PointerByReference iter = OpingLibrary.ping_iterator_get(opingObject);
		while(iter!=null)
		{
			OpingLibrary.ping_iterator_get_info(iter, OpingLibrary.PING_INFO_USERNAME, m, ml);
			String address = m.getString(0);
			nl.setValue(128L);
			ml.setValue(nl);
			OpingLibrary.ping_iterator_get_info(iter, OpingLibrary.PING_INFO_DROPPED, m, ml);
			int dropped = m.getInt(0);
			nl.setValue(128L);
			ml.setValue(nl);
			OpingLibrary.ping_iterator_get_info(iter, OpingLibrary.PING_INFO_LATENCY, m, ml);
			double latency = m.getDouble(0);
			nl.setValue(128L);
			ml.setValue(nl);
			for(OpingObserver ob : this.observer)
			{
				ob.observe(address, 1, dropped, this.timeout, latency);
			}
			iter = OpingLibrary.ping_iterator_next(iter);
		}
	}

	@Override
	public void observe(String address, int i, int dropped, double timeout, double latency)
	{
		System.out.println(String.format("recv from %s: loss=%f%%, time=%f ms",address,100.0-((double)(i-dropped)*100.0)/((double)i), latency));
	}

	public int getRetries()
	{
		return retries;
	}

	public void setRetries(int retries)
	{
		this.retries = retries;
	}

	public double getTimeout()
	{
		return timeout;
	}

	public void setTimeout(double timeout)
	{
		this.timeout = timeout;
	}

	public int getTtl()
	{
		return ttl;
	}

	public void setTtl(int ttl)
	{
		this.ttl = ttl;
	}

	public int getAf()
	{
		return af;
	}

	public void setAf(int af)
	{
		this.af = af;
	}

	public String getData()
	{
		return data;
	}

	public void setData(String data)
	{
		this.data = data;
	}

	public String getSource()
	{
		return source;
	}

	public void setSource(String source)
	{
		this.source = source;
	}

	public String getDevice()
	{
		return device;
	}

	public void setDevice(String device)
	{
		this.device = device;
	}

	public Set<OpingObserver> getObserver()
	{
		return observer;
	}

	public void setObserver(Set<OpingObserver> observer)
	{
		this.observer = observer;
	}

	public static void main(String[] args) throws Exception
	{
		Opinger op = new Opinger();
		op.setRetries(2);
		op.setTimeout(5.0);
		op.setTtl(64);
		op.getObserver().add(op);

		for(String n : args)
		{
			op.addAddress(n);
		}

		if(args.length==0)
		{
			op.addAddress("127.0.0.1");
		}

		while(true)
		{
			op.init();
			op.runPingOnce();
			op.runPostOnce();
			op.close();
			Thread.sleep(500L);
		}
	}
}
