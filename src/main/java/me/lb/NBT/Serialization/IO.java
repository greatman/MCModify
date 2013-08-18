/******************************************************************************\
|* Copyright © 2013 LB-Stuff                                                  *|
|* All rights reserved.                                                       *|
|*                                                                            *|
|* Redistribution and use in source and binary forms, with or without         *|
|* modification, are permitted provided that the following conditions         *|
|* are met:                                                                   *|
|*                                                                            *|
|*  1. Redistributions of source code must retain the above copyright         *|
|*     notice, this list of conditions and the following disclaimer.          *|
|*                                                                            *|
|*  2. Redistributions in binary form must reproduce the above copyright      *|
|*     notice, this list of conditions and the following disclaimer in the    *|
|*     documentation and/or other materials provided with the distribution.   *|
|*                                                                            *|
|* THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS "AS IS" AND       *|
|* ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE      *|
|* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE *|
|* ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE    *|
|* FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL *|
|* DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS    *|
|* OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)      *|
|* HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT *|
|* LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY  *|
|* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF     *|
|* SUCH DAMAGE.                                                               *|
\******************************************************************************/

package me.lb.NBT.Serialization;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import me.lb.NBT.FormatException;
import me.lb.NBT.Tag;

/**
 * This class can serialize and deserialize classes that implement <code>NBTable</code> to/from NBT structures.
 * @author LB
 */
public final class IO
{
	/**
	 * Serializes an instance of an NBTable class to a compound tag by serializing all non-transient fields that it can, including those of any NBTable super classes; fields that are null will be ignored. It can handle the primitive types, strings, other NBTable objects, and up to one dimensional arrays of such. It will also serialize fields of type Class<?> by saving the name of the class. Additionally, if one of the classes has an instance method called "ToNBT" that takes only a String for its name as a parameter and returns a Tag, that will also be serialized. Here is an example declaration:<br />
	 * <code>public Tag ToNBT(String name);</code>
	 * @param name The name of the compound tag to return.
	 * @param obj The object to serialize to the compound tag.
	 * @param preferList Whether to prefer TAG_List over TAG_Byte_Array and TAG_Int_Array when faced with byte[] and int[] types.
	 * @return The compound tag representing the fully serialized NBTable object.
	 * @throws IllegalAccessException in rare cases.
	 */
	public static Tag.Compound Serialize(String name, NBTable obj, boolean preferList) throws IllegalAccessException
	{
		return Serialize(name, obj.getClass(), obj, preferList);
	}
	/**
	 * The recursive method that does all the work for the above public method; used to hide the ugly Class parameter.
	 * @param name Same as above.
	 * @param clazz The class level at which to serialize the fields of.
	 * @param obj Same as above.
	 * @param preferList Same as above.
	 * @return Same as above.
	 * @throws IllegalAccessException in the same cases as above.
	 */
	private static Tag.Compound Serialize(String name, Class<? extends NBTable> clazz, NBTable obj, boolean preferList) throws IllegalAccessException
	{
		Class<?> sup = clazz.getSuperclass();
		Tag.Compound t;
		if(sup != null && NBTable.class.isAssignableFrom(sup))
		{
			t = Serialize(name, (Class<? extends NBTable>)sup, obj, preferList);
		}
		else
		{
			t = new Tag.Compound(name);
		}
		for(Field field : clazz.getDeclaredFields())
		{
			field.setAccessible(true);
			if(!Modifier.isTransient(field.getModifiers()))
			{
				Object o = field.get(obj);
				if(o == null)
				{
					continue;
				}
				Class<?> c = field.getClass();
				String n = field.getName();
				if(o instanceof NBTable)
				{
					t.Add(Serialize(n, (NBTable)o, preferList));
				}
				else if(o instanceof Byte)
				{
					t.Add(new Tag.Byte(n, (Byte)o));
				}
				else if(o instanceof Short)
				{
					t.Add(new Tag.Short(n, (Short)o));
				}
				else if(o instanceof Integer)
				{
					t.Add(new Tag.Int(n, (Integer)o));
				}
				else if(o instanceof Long)
				{
					t.Add(new Tag.Long(n, (Long)o));
				}
				else if(o instanceof Float)
				{
					t.Add(new Tag.Float(n, (Float)o));
				}
				else if(o instanceof Double)
				{
					t.Add(new Tag.Double(n, (Double)o));
				}
				else if(o instanceof String)
				{
					t.Add(new Tag.String(n, (String)o));
				}
				else if(NBTable[].class.isAssignableFrom(c))
				{
					Tag.List list = new Tag.List(n, Tag.Type.COMPOUND);
					try
					{
						for(NBTable nbtable : (NBTable[])o)
						{
							list.Add(Serialize(null, nbtable, preferList));
						}
						t.Add(list);
					}
					catch(Tag.Type.MismatchException e)
					{
					}
				}
				else if(o instanceof byte[])
				{
					if(preferList)
					{
						Tag.List list = new Tag.List(n, Tag.Type.BYTE);
						try
						{
							for(byte b : (byte[])o)
							{
								list.Add(new Tag.Byte(null, b));
							}
						t.Add(list);
						}
						catch(Tag.Type.MismatchException e)
						{
						}
					}
					else
					{
						t.Add(new Tag.ByteArray(n, (byte[])o));
					}
				}
				else if(o instanceof short[])
				{
					Tag.List list = new Tag.List(n, Tag.Type.SHORT);
					try
					{
						for(short s : (short[])o)
						{
							list.Add(new Tag.Short(null, s));
						}
						t.Add(list);
					}
					catch(Tag.Type.MismatchException e)
					{
					}
				}
				else if(o instanceof int[])
				{
					if(preferList)
					{
						Tag.List list = new Tag.List(n, Tag.Type.INT);
						try
						{
							for(int i : (int[])o)
							{
								list.Add(new Tag.Int(null, i));
							}
							t.Add(list);
						}
						catch(Tag.Type.MismatchException e)
						{
						}
					}
					else
					{
						t.Add(new Tag.IntArray(n, (int[])o));
					}
				}
				else if(o instanceof long[])
				{
					Tag.List list = new Tag.List(n, Tag.Type.LONG);
					try
					{
						for(long l : (long[])o)
						{
							list.Add(new Tag.Long(null, l));
						}
						t.Add(list);
					}
					catch(Tag.Type.MismatchException e)
					{
					}
				}
				else if(o instanceof float[])
				{
					Tag.List list = new Tag.List(n, Tag.Type.FLOAT);
					try
					{
						for(float f : (float[])o)
						{
							list.Add(new Tag.Float(null, f));
						}
						t.Add(list);
					}
					catch(Tag.Type.MismatchException e)
					{
					}
				}
				else if(o instanceof double[])
				{
					Tag.List list = new Tag.List(n, Tag.Type.DOUBLE);
					try
					{
						for(double d : (double[])o)
						{
							list.Add(new Tag.Double(null, d));
						}
						t.Add(list);
					}
					catch(Tag.Type.MismatchException e)
					{
					}
				}
				else if(o instanceof String[])
				{
					Tag.List list = new Tag.List(n, Tag.Type.STRING);
					try
					{
						for(String s : (String[])o)
						{
							list.Add(new Tag.String(null, s));
						}
						t.Add(list);
					}
					catch(Tag.Type.MismatchException e)
					{
					}
				}
				else if(o instanceof Class<?>)
				{
					t.Add(new Tag.String(n, ((Class<?>)o).getCanonicalName()));
				}
				else
				{
					try
					{
						Method m = c.getDeclaredMethod("ToNBT", String.class);
						m.setAccessible(true);
						if(Tag.class.isAssignableFrom(m.getReturnType()))
						{
							t.Add((Tag)m.invoke(o, n));
						}
					}
					catch(NoSuchMethodException|InvocationTargetException e)
					{
					}
				}
			}
		}
		return t;
	}
	/**
	 * Deserializes a compound tag and returns a new instance of the given class. In order to construct the instance the given class must have a constructor that takes a <code>Tag.Compound</code> as its only parameter. The constructor must initialize any null fields to the bare minimum of an instantiated class of the correct type if the fields are to be deserialized, otherwise null fields are ignored. See {@url Serialize} for information.
	 * @param clazz The class to deserialize.
	 * @param outer The outer class reference to use to construct non-static inner classes. May be null if the given class is not a non-static inner class.
	 * @param t The tag to deserialize from.
	 * @return An instance of the deserialized class.
	 * @throws IllegalArgumentException if this method cannot find a way to properly instantiate the given class.
	 * @throws NoSuchMethodException If the required constructor for deserialization is not found.
	 * @throws InstantiationException if the required constructor throws an exception at instantiation.
	 * @throws IllegalAccessException in rare cases.
	 * @throws InvocationTargetException if the required constructor throws an exception.
	 * @throws FormatException if a required tag cannot be found to deserialize to the class.
	 * @throws ClassNotFoundException if a class object cannot be found to deserialize to Class fields.
	 */
	public static NBTable Deserialize(Class<? extends NBTable> clazz, Object outer, Tag.Compound t) throws IllegalArgumentException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FormatException, ClassNotFoundException
	{
		Class<?> out;
		Constructor<? extends NBTable> cons;
		NBTable obj;
		if((out = clazz.getDeclaringClass()) != null && !Modifier.isStatic(clazz.getModifiers()))
		{
			cons = clazz.getDeclaredConstructor(out, Tag.Compound.class);
			cons.setAccessible(true);
			obj = cons.newInstance(outer, t);
		}
		else if(out == null || Modifier.isStatic(clazz.getModifiers()))
		{
			cons = clazz.getDeclaredConstructor(Tag.Compound.class);
			cons.setAccessible(true);
			obj = cons.newInstance(t);
		}
		else
		{
			throw new IllegalArgumentException("Can't tell how to construct this class...");
		}
		Deserialize(clazz, obj, t);
		return obj;
	}
	/**
	 * The recursive method that does all the work for the above public method; used to hide the ugly NBTable parameter.
	 * @param clazz Same as above.
	 * @param obj The object to deserialize to.
	 * @param t Same as above.
	 * @throws NoSuchMethodException in the same cases as above.
	 * @throws InstantiationException in the same cases as above.
	 * @throws IllegalAccessException in the same cases as above.
	 * @throws InvocationTargetException in the same cases as above.
	 * @throws FormatException in the same cases as above.
	 * @throws ClassNotFoundException in the same cases as above.
	 */
	private static void Deserialize(Class<? extends NBTable> clazz, NBTable obj, Tag.Compound t) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FormatException, ClassNotFoundException
	{
		Class<?> sup = clazz.getSuperclass();
		if(sup != null && NBTable.class.isAssignableFrom(sup))
		{
			Deserialize((Class<? extends NBTable>)sup, obj, t);
		}
	FieldLoop:
		for(Field field : clazz.getDeclaredFields())
		{
			field.setAccessible(true);
			if(!Modifier.isTransient(field.getModifiers()))
			{
				Object o = field.get(obj);
				if(o == null)
				{
					continue;
				}
				Class<?> c = field.getClass();
				String n = field.getName();
				if(o instanceof NBTable)
				{
					field.set(obj, Deserialize(c.asSubclass(NBTable.class), (Object)null, (Tag.Compound)t.Find(Tag.Type.COMPOUND, n)));
				}
				else if(o instanceof Byte)
				{
					field.set(obj, ((Tag.Byte)t.Find(Tag.Type.BYTE, n)).v);
				}
				else if(o instanceof Short)
				{
					field.set(obj, ((Tag.Short)t.Find(Tag.Type.SHORT, n)).v);
				}
				else if(o instanceof Integer)
				{
					field.set(obj, ((Tag.Int)t.Find(Tag.Type.INT, n)).v);
				}
				else if(o instanceof Long)
				{
					field.set(obj, ((Tag.Long)t.Find(Tag.Type.LONG, n)).v);
				}
				else if(o instanceof Float)
				{
					field.set(obj, ((Tag.Float)t.Find(Tag.Type.FLOAT, n)).v);
				}
				else if(o instanceof Double)
				{
					field.set(obj, ((Tag.Double)t.Find(Tag.Type.DOUBLE, n)).v);
				}
				else if(o instanceof String)
				{
					field.set(obj, ((Tag.String)t.Find(Tag.Type.STRING, n)).v);
				}
				else if(NBTable[].class.isAssignableFrom(c))
				{
					Tag.List list = (Tag.List)t.Find(Tag.Type.LIST, n);
					if(list.Supports() != Tag.Type.COMPOUND)
					{
						throw new FormatException("Expected list of Compound tags, got list of: "+list.Supports());
					}
					NBTable[] arr = (NBTable[])Array.newInstance(c.getComponentType(), list.Size());
					for(int i = 0; i < arr.length; ++i)
					{
						arr[i] = Deserialize(c.asSubclass(NBTable.class), (Object)null, (Tag.Compound)list.Get(i));
					}
					field.set(obj, arr);
				}
				else if(o instanceof byte[])
				{
					for(Tag list : t)
					{
						if(n.equals(list.Name()))
						{
							Tag.List tl;
							if(list.Type() == Tag.Type.BYTEARRAY)
							{
								byte[] arr = ((Tag.ByteArray)list).v;
								field.set(obj, Arrays.copyOf(arr, arr.length));
								continue FieldLoop;
							}
							else if(list.Type() == Tag.Type.LIST && (tl = (Tag.List)list).Supports() == Tag.Type.BYTE)
							{
								byte[] arr = new byte[tl.Size()];
								for(int j = 0; j < tl.Size(); ++j)
								{
									arr[j] = ((Tag.Byte)tl.Get(j)).v;
								}
								field.set(obj, arr);
								continue FieldLoop;
							}
						}
					}
					throw new FormatException("Could not find a ByteArray tag or List of Byte tags with the name \""+n+"\"");
				}
				else if(o instanceof short[])
				{
					Tag.List list = (Tag.List)t.Find(Tag.Type.LIST, n);
					if(list.Supports() != Tag.Type.SHORT)
					{
						throw new FormatException("Expected list of Short tags, got list of: "+list.Supports());
					}
					short[] arr = new short[list.Size()];
					for(int i = 0; i < list.Size(); ++i)
					{
						arr[i] = ((Tag.Short)list.Get(i)).v;
					}
				}
				else if(o instanceof int[])
				{
					for(Tag list : t)
					{
						if(n.equals(list.Name()))
						{
							Tag.List tl;
							if(list.Type() == Tag.Type.INTARRAY)
							{
								int[] arr = ((Tag.IntArray)list).v;
								field.set(obj, Arrays.copyOf(arr, arr.length));
								continue FieldLoop;
							}
							else if(list.Type() == Tag.Type.LIST && (tl = (Tag.List)list).Supports() == Tag.Type.INT)
							{
								int[] arr = new int[tl.Size()];
								for(int j = 0; j < tl.Size(); ++j)
								{
									arr[j] = ((Tag.Int)tl.Get(j)).v;
								}
								field.set(obj, arr);
								continue FieldLoop;
							}
						}
					}
					throw new FormatException("Could not find an IntArray tag or List of Int tags with the name \""+n+"\"");
				}
				else if(o instanceof long[])
				{
					Tag.List list = (Tag.List)t.Find(Tag.Type.LIST, n);
					if(list.Supports() != Tag.Type.LONG)
					{
						throw new FormatException("Expected list of Long tags, got list of: "+list.Supports());
					}
					long[] arr = new long[list.Size()];
					for(int i = 0; i < list.Size(); ++i)
					{
						arr[i] = ((Tag.Long)list.Get(i)).v;
					}
				}
				else if(o instanceof float[])
				{
					Tag.List list = (Tag.List)t.Find(Tag.Type.LIST, n);
					if(list.Supports() != Tag.Type.FLOAT)
					{
						throw new FormatException("Expected list of Float tags, got list of: "+list.Supports());
					}
					float[] arr = new float[list.Size()];
					for(int i = 0; i < list.Size(); ++i)
					{
						arr[i] = ((Tag.Float)list.Get(i)).v;
					}
				}
				else if(o instanceof double[])
				{
					Tag.List list = (Tag.List)t.Find(Tag.Type.LIST, n);
					if(list.Supports() != Tag.Type.DOUBLE)
					{
						throw new FormatException("Expected list of Double tags, got list of: "+list.Supports());
					}
					double[] arr = new double[list.Size()];
					for(int i = 0; i < list.Size(); ++i)
					{
						arr[i] = ((Tag.Double)list.Get(i)).v;
					}
				}
				else if(o instanceof String[])
				{
					Tag.List list = (Tag.List)t.Find(Tag.Type.LIST, n);
					if(list.Supports() != Tag.Type.STRING)
					{
						throw new FormatException("Expected list of String tags, got list of: "+list.Supports());
					}
					String[] arr = new String[list.Size()];
					for(int i = 0; i < list.Size(); ++i)
					{
						arr[i] = ((Tag.String)list.Get(i)).v;
					}
				}
				else if(o instanceof Class<?>)
				{
					field.set(obj, Class.forName(((Tag.String)t.Find(Tag.Type.STRING, n)).v));
				}
				else
				{
					Constructor<?> subcons = c.getConstructor(Tag.class);
					subcons.setAccessible(true);
					field.set(obj, subcons.newInstance(t.Get(n)));
				}
			}
		}
	}

	/**
	 * The constructor for this class, which you won't be using.
	 * @throws UnsupportedOperationException always.
	 */
	private IO() throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}
}