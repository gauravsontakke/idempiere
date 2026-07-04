/******************************************************************************
 * Copyright (C) 2024 iDempiere contributors. All Rights Reserved.           *
 * This program is free software; you can redistribute it and/or modify it   *
 * under the terms version 2 of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope  *
 * that it will be useful, but WITHOUT ANY WARRANTY.                         *
 *****************************************************************************/
package org.compiere.util;

import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * Wraps ICU4J's DecimalFormat as a java.text.DecimalFormat to support locales
 * with secondary grouping (e.g., en_IN uses Indian-style 1,35,000.00 grouping).
 * Java 17's standard DecimalFormat ignores secondary grouping; ICU4J does not.
 *
 * Note: setMaximumFractionDigits / setMinimumFractionDigits / setMaximumIntegerDigits /
 * setMinimumIntegerDigits are final in java.text.DecimalFormat and cannot be overridden.
 * Instead, syncToICU() pushes the parent's current digit limits into icuFormat
 * immediately before every format() call so the two are always consistent.
 */
public class ICUDecimalFormatWrapper extends java.text.DecimalFormat {

	private static final long serialVersionUID = 1L;

	private final com.ibm.icu.text.DecimalFormat icuFormat;
	private final Locale javaLocale;

	public ICUDecimalFormatWrapper(com.ibm.icu.text.DecimalFormat icuFormat, Locale javaLocale) {
		super("#,##0.##");
		this.icuFormat = icuFormat;
		this.javaLocale = javaLocale;
		icuFormat.setParseBigDecimal(true);
		syncDecimalSymbols();
		// Initialise parent's digit limits from ICU so getters are correct from the start.
		// These are the final methods in DecimalFormat — calling them here is fine.
		setMaximumFractionDigits(icuFormat.getMaximumFractionDigits());
		setMinimumFractionDigits(icuFormat.getMinimumFractionDigits());
		setMaximumIntegerDigits(icuFormat.getMaximumIntegerDigits());
		setMinimumIntegerDigits(icuFormat.getMinimumIntegerDigits());
	}

	/**
	 * Push the parent's current digit limits and grouping flags into icuFormat.
	 * Called before every format() because setMaximumFractionDigits et al. are
	 * final in DecimalFormat and cannot be intercepted via override.
	 */
	private void syncToICU() {
		icuFormat.setMaximumIntegerDigits(super.getMaximumIntegerDigits());
		icuFormat.setMinimumIntegerDigits(super.getMinimumIntegerDigits());
		icuFormat.setMaximumFractionDigits(super.getMaximumFractionDigits());
		icuFormat.setMinimumFractionDigits(super.getMinimumFractionDigits());
		icuFormat.setGroupingUsed(super.isGroupingUsed());
		icuFormat.setParseIntegerOnly(super.isParseIntegerOnly());
	}

	private void syncDecimalSymbols() {
		com.ibm.icu.text.DecimalFormatSymbols icuSymbols = icuFormat.getDecimalFormatSymbols();
		java.text.DecimalFormatSymbols javaSymbols = new java.text.DecimalFormatSymbols(javaLocale);
		javaSymbols.setDecimalSeparator(icuSymbols.getDecimalSeparator());
		javaSymbols.setGroupingSeparator(icuSymbols.getGroupingSeparator());
		javaSymbols.setMinusSign(icuSymbols.getMinusSign());
		javaSymbols.setZeroDigit(icuSymbols.getZeroDigit());
		javaSymbols.setInfinity(icuSymbols.getInfinity());
		javaSymbols.setNaN(icuSymbols.getNaN());
		setDecimalFormatSymbols(javaSymbols);
	}

	@Override
	public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
		syncToICU();
		result.append(icuFormat.format(number));
		return result;
	}

	@Override
	public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
		syncToICU();
		result.append(icuFormat.format(number));
		return result;
	}

	@Override
	public Number parse(String text, ParsePosition pos) {
		return icuFormat.parse(text, pos);
	}

	@Override
	public int getGroupingSize() {
		return icuFormat.getGroupingSize();
	}

	@Override
	public void setGroupingSize(int newValue) {
		icuFormat.setGroupingSize(newValue);
	}

	@Override
	public String toPattern() {
		return icuFormat.toPattern();
	}

	@Override
	public String toLocalizedPattern() {
		return icuFormat.toLocalizedPattern();
	}

	@Override
	public void applyPattern(String pattern) {
		icuFormat.applyPattern(pattern);
		syncDecimalSymbols();
	}

	@Override
	public void applyLocalizedPattern(String pattern) {
		icuFormat.applyLocalizedPattern(pattern);
		syncDecimalSymbols();
	}

	@Override
	public DecimalFormatSymbols getDecimalFormatSymbols() {
		return toJavaSymbols(icuFormat.getDecimalFormatSymbols(), javaLocale);
	}

	private static java.text.DecimalFormatSymbols toJavaSymbols(
			com.ibm.icu.text.DecimalFormatSymbols icuSymbols, Locale javaLocale) {
		java.text.DecimalFormatSymbols javaSymbols = new java.text.DecimalFormatSymbols(javaLocale);
		javaSymbols.setDecimalSeparator(icuSymbols.getDecimalSeparator());
		javaSymbols.setGroupingSeparator(icuSymbols.getGroupingSeparator());
		javaSymbols.setMinusSign(icuSymbols.getMinusSign());
		javaSymbols.setZeroDigit(icuSymbols.getZeroDigit());
		javaSymbols.setInfinity(icuSymbols.getInfinity());
		javaSymbols.setNaN(icuSymbols.getNaN());
		return javaSymbols;
	}
}
