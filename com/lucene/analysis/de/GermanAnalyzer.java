package com.lucene.analysis.de;

import com.lucene.analysis.Analyzer;
import com.lucene.analysis.LowerCaseFilter;
import com.lucene.analysis.StopFilter;
import com.lucene.analysis.TokenStream;
import com.lucene.analysis.standard.StandardFilter;
import com.lucene.analysis.standard.StandardTokenizer;
import java.io.File;
import java.io.Reader;
import java.util.Hashtable;

/**
 * Analyzer for german language. Supports an external list of stopwords (words that
 * will not be indexed at all) and an external list of exclusions (word that will
 * not be stemmed, but indexed).
 *
 * @author    Gerhard Schwarz
 * @version   $Id: GermanAnalyzer.java,v 1.1 2001/09/24 18:01:05 cutting Exp $
 */
public final class GermanAnalyzer extends Analyzer {

	/**
	 * List of typical german stopwords.
	 */
	private String[] GERMAN_STOP_WORDS = {
		"einer", "eine", "eines", "einem", "einen",
		"der", "die", "das", "dass", "daß",
		"du", "er", "sie", "es",
		"was", "wer", "wie", "wir",
		"und", "oder", "ohne", "mit",
		"am", "im", "in", "aus", "auf",
		"ist", "sein", "war", "wird",
		"ihr", "ihre", "ihres",
		"als", "für", "von", "mit",
		"dich", "dir", "mich", "mir",
		"mein", "sein", "kein",
		"durch", "wegen"
		};
	
	/**
	 * Contains the stopwords used with the StopFilter.
	 */
	private Hashtable stoptable = new Hashtable();
	/**
	 * Contains words that should be indexed but not stemmed.
	 */
	private Hashtable excltable = new Hashtable();
	
	/**
	 * Builds an analyzer.
	 */
	public GermanAnalyzer() {
		stoptable = StopFilter.makeStopTable( GERMAN_STOP_WORDS );
	}

	/**
	 * Builds an analyzer with the given stop words.
	 */
	public GermanAnalyzer( String[] stopwords ) {
		stoptable = StopFilter.makeStopTable( stopwords );
	}

	/**
	 * Builds an analyzer with the given stop words.
	 */
	public GermanAnalyzer( Hashtable stopwords ) {
		stoptable = stopwords;
	}

	/**
	 * Builds an analyzer with the given stop words.
	 */
	public GermanAnalyzer( File stopwords ) {
		stoptable = WordlistLoader.getWordtable( stopwords );
	}

	/**
	 * Builds an exclusionlist from an array of Strings.
	 */
	public void setStemExclusionTable( String[] exclusionlist ) {
		excltable = StopFilter.makeStopTable( exclusionlist );
	}
	/**
	 * Builds an exclusionlist from a Hashtable.
	 */
	public void setStemExclusionTable( Hashtable exclusionlist ) {
		excltable = exclusionlist;
	}
	/**
	 * Builds an exclusionlist from the words contained in the given file.
	 */
	public void setStemExclusionTable( File exclusionlist ) {
		excltable = WordlistLoader.getWordtable( exclusionlist );
	}
	
	/**
	 * Creates a TokenStream which tokenizes all the text in the provided Reader.
	 *
	 * @return  A TokenStream build from a StandardTokenizer filtered with
	 * 			StandardFilter, StopFilter, GermanStemFilter and LowerCaseFilter.
	 */
	public final TokenStream tokenStream(String fieldName, Reader reader) {
		TokenStream result = new StandardTokenizer( reader );
		result = new StandardFilter( result );
		result = new StopFilter( result, stoptable );
		result = new GermanStemFilter( result, excltable );
		// Convert to lowercase after stemming!
		result = new LowerCaseFilter( result );
		return result;
	}
}

