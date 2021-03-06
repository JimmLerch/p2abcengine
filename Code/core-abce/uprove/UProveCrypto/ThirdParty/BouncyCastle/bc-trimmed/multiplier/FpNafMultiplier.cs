//*********************************************************
//
// This file was imported from the C# Bouncy Castle project. Original license header is retained:
//
//
// License
// Copyright (c) 2000 - 2010 The Legion Of The Bouncy Castle (http://www.bouncycastle.org) 
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
//
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. 
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
//
//*********************************************************

namespace BouncyCastle
{
	/**
	* Class implementing the NAF (Non-Adjacent Form) multiplication algorithm.
	*/
	internal class FpNafMultiplier
		: ECMultiplier
	{
		/**
		* D.3.2 pg 101
		* @see org.bouncycastle.math.ec.multiplier.ECMultiplier#multiply(org.bouncycastle.math.ec.ECPoint, java.math.BigInteger)
		*/
		public ECPoint Multiply(ECPoint p, BigInteger k, PreCompInfo preCompInfo)
		{
			// TODO Probably should try to add this
			// BigInteger e = k.Mod(n); // n == order of p
			BigInteger e = k;
			BigInteger h = e.Multiply(BigInteger.Three);

			ECPoint neg = p.Negate();
			ECPoint R = p;

			for (int i = h.BitLength - 2; i > 0; --i)
			{             
				R = R.Twice();

				bool hBit = h.TestBit(i);
				bool eBit = e.TestBit(i);

				if (hBit != eBit)
				{
					R = R.Add(hBit ? p : neg);
				}
			}

			return R;
		}
	}
}
