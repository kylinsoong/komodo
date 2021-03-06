/*
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.komodo.shell;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.komodo.shell.Messages.SHELL;
import org.komodo.shell.api.AbstractShellCommand;
import org.komodo.shell.api.Arguments;
import org.komodo.shell.api.WorkspaceContext;
import org.komodo.shell.api.WorkspaceStatus;
import org.komodo.shell.util.ContextUtils;
import org.komodo.spi.constants.StringConstants;
import org.komodo.spi.repository.KomodoType;
import org.komodo.utils.ArgCheck;
import org.komodo.utils.StringNameValidator;
import org.komodo.utils.StringUtils;

/**
 * Abstract base class for all built-in shell commands.
 *
 * This class adapted from https://github.com/Governance/s-ramp/blob/master/s-ramp-shell
 * - altered to use different Messages class
 *
 */
public abstract class BuiltInShellCommand extends AbstractShellCommand {

    /**
     * @param context
     *        the associated context (cannot be null)
     * @param propertyName
     *        the name whose namespace prefix is being attached (cannot be empty)
     * @return the property name with the namespace prefix attached (never empty)
     * @throws Exception
     *         if an error occurs
     */
    protected static String attachPrefix( final WorkspaceContext context,
                                          final String propertyName ) throws Exception {
        ArgCheck.isNotNull( context, "context" ); //$NON-NLS-1$
        ArgCheck.isNotEmpty( propertyName, "propertyName" ); //$NON-NLS-1$

        for ( final String name : context.getProperties() ) {
            if ( propertyName.equals( removePrefix( name ) ) ) {
                return name;
            }
        }

        return propertyName;
    }

    /**
     * @param propertyName
     *        the property name whose namespace prefix is being removed (cannot be empty)
     * @return the name without the namespace prefix (never empty)
     */
    protected static String removePrefix( final String propertyName ) {
        ArgCheck.isNotEmpty( propertyName, "qname" ); //$NON-NLS-1$
        final int index = propertyName.indexOf( ':' );

        if ( index == -1 ) {
            return propertyName;
        }

        if ( index < propertyName.length() ) {
            return propertyName.substring( index + 1 );
        }

        return propertyName;
    }

	private final StringNameValidator nameValidator = new StringNameValidator();
    private final String name;
    private final String[] aliases;

    /**
     * Constructs a command.
     *
     * @param status
     *        the workspace status (cannot be <code>null</code>)
     * @param names
     *        the command name and then any aliases (cannot be <code>null</code>, empty, or have a <code>null</code> first
     *        element)
     */
	public BuiltInShellCommand(final WorkspaceStatus status,
	                           final String... names ) {
		super(status);

		ArgCheck.isNotEmpty( names, "names" ); //$NON-NLS-1$
        ArgCheck.isNotEmpty( names[0], "names[0]" ); //$NON-NLS-1$

        this.name = names[0];

        // save aliases if necessary
        if ( names.length == 1 ) {
            this.aliases = StringConstants.EMPTY_ARRAY;
        } else {
            this.aliases = new String[ names.length - 1 ];
            boolean firstTime = true;
            int i = 0;

            for ( final String alias : names ) {
                if (firstTime) {
                    firstTime = false;
                    continue;
                }

                this.aliases[i++] = alias;
            }
        }

        initValidWsContextTypes();
	}

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.api.ShellCommand#getAliases()
     */
    @Override
    public final String[] getAliases() {
        return this.aliases;
    }

	protected WorkspaceContext getContext() {
	    return getWorkspaceStatus().getCurrentContext();
	}

    /**
     * @see org.komodo.shell.api.ShellCommand#getName()
     */
    @Override
    public final String getName() {
        return this.name;
    }

	protected boolean isShowingPropertyNamePrefixes() {
	    return getWorkspaceStatus().isShowingPropertyNamePrefixes();
	}

    /**
     * @see org.komodo.shell.api.ShellCommand#printUsage(int indent)
     */
    @Override
    public void printUsage( final int indent ) {
        print( indent, Messages.getString( SHELL.HelpUsageHeading ) );
        printHelpUsage( 2 * indent );
    }

    private void printAliases( final int indent ) {
        final int twoIndents = ( 2 * indent );
        final String[] aliases = getAliases();

        if ( aliases.length == 0 ) {
            print( twoIndents, Messages.getString( SHELL.HelpNoAliases ) );
        } else {
            final StringBuilder builder = new StringBuilder();
            boolean firstTime = true;

            for ( final String alias : aliases ) {
                if ( firstTime ) {
                    firstTime = false;
                } else {
                    builder.append( ", " ); //$NON-NLS-1$
                }

                builder.append( alias );
            }

            print( twoIndents, builder.toString() );
        }
    }

    /**
     * @see org.komodo.shell.api.ShellCommand#printHelp(int indent)
     */
    @Override
    public void printHelp( final int indent ) {
        // description
        print( indent, Messages.getString( SHELL.HelpDescriptionHeading ) );
        printHelpDescription( indent );
        print();

        // aliases
        print( indent, Messages.getString( SHELL.HelpAliasesHeading ) );
        printAliases( indent );
        print();

        // usage
        printUsage( indent );
        print();

        // examples
        print( indent, Messages.getString( SHELL.HelpExamplesHeading ) );
        printHelpExamples( indent );
    }

    protected void printHelpDescription( final int indent ) {
        print( indent,
               Messages.getString( SHELL.HelpDescription, getName(), Messages.getString( getClass().getSimpleName() + ".help" ) ) ); //$NON-NLS-1$
    }

    protected void printHelpExamples( final int indent ) {
        print( indent, Messages.getString( getClass().getSimpleName() + ".examples" ) ); //$NON-NLS-1$
    }

    protected void printHelpUsage( final int indent ) {
        print( indent, Messages.getString( getClass().getSimpleName() + ".usage" ) ); //$NON-NLS-1$
    }

    protected void print() {
        print( 0, StringConstants.EMPTY_STRING );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.api.ShellCommand#record()
     */
    @Override
    public void record() {
    	recordToFile(toString());
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final Arguments args = getArguments();
        final StringBuffer buff = new StringBuffer( getName() );

        for ( int i = 0, size = args.size(); i < size; ++i ) {
            buff.append(' ').append(args.get( i ) );
        }

        return buff.toString();
    }

    /**
     * @see org.komodo.shell.api.ShellCommand#recordComment(String)
     */
    @Override
    public void recordComment(String comment) {
    	recordToFile("# "+comment);  //$NON-NLS-1$
    }

    /**
     * Write the supplied line to the recording output file.
     * @param line the line to output
     */
    private void recordToFile(String line) {
    	File outputFile = getWorkspaceStatus().getRecordingOutputFile();
    	if(outputFile!=null) {
    		FileWriter recordingFileWriter = null;
    		try {
    			// Create file if it doesnt exist
            	outputFile.createNewFile();
				recordingFileWriter = new FileWriter(outputFile,true);
				recordingFileWriter.write(line+"\n"); //$NON-NLS-1$
				recordingFileWriter.flush();
			} catch (IOException ex) {
	            print(0,"*** Could not create or write to the specifed recording file - "+outputFile); //$NON-NLS-1$
			}
    	    finally {
    	        try {
    	        	recordingFileWriter.close();
    	        } catch (final IOException ignored) {
    	            // ignore
    	        }
    	    }
        // Print error message if the recording file was not defined
    	} else {
            print(0,"*** Recording file not defined in startup properties"); //$NON-NLS-1$
    	}
    }

    /**
     * Validate whether the supplied name is valid for the supplied type
     * @param name the name
     * @param kType the komodo object type
     * @return 'true' if valid, 'false' if not.
     */
    protected boolean validateObjectName(String name, KomodoType kType) {
    	boolean isValid = nameValidator.isValidName(name);
    	if(!isValid) {
            print(CompletionConstants.MESSAGE_INDENT,Messages.getString("BuiltInShellCommand.objectNameNotValid",name)); //$NON-NLS-1$
    		return false;
    	}
        return true;
    }

	/**
	 * Validates that the supplied path argument is a readable file.
	 * @param filePathArg the path to test
	 * @return 'true' if the path is valid readable file, 'false' if not.
	 */
	public boolean validateReadableFileArg(String filePathArg) {
		String filePath = filePathArg.trim();

        // Check the fileName arg validity
        File theFile = new File(filePath);
        if(!theFile.exists()) {
        	print(CompletionConstants.MESSAGE_INDENT, Messages.getString("BuiltInShellCommand.FileNotFound", filePath)); //$NON-NLS-1$
        	return false;
        } else if(!theFile.isFile()) {
        	print(CompletionConstants.MESSAGE_INDENT, Messages.getString("BuiltInShellCommand.FileArgNotAFile", filePath)); //$NON-NLS-1$
        	return false;
        } else if(!theFile.canRead()) {
        	print(CompletionConstants.MESSAGE_INDENT, Messages.getString("BuiltInShellCommand.FileNotReadable", filePath)); //$NON-NLS-1$
        	return false;
        }

		return true;
	}

	/**
	 * Validates whether the supplied path is valid.  If the path is relative this takes into account the
	 * current context.  If invalid an error message is printed out.
	 * @param pathArg the path to test
	 * @return 'true' if the path is valid, 'false' if not.
	 */
	public boolean validatePath(String pathArg) {
		String path = pathArg.trim();
		if(path.length()==0) {
            print(CompletionConstants.MESSAGE_INDENT,Messages.getString("BuiltInShellCommand.locationArg_empty")); //$NON-NLS-1$
			return false;
		}

		WorkspaceStatus wsStatus = getWorkspaceStatus();
		WorkspaceContext newContext = ContextUtils.getContextForPath(wsStatus, pathArg);

		if(newContext==null) {
            print(CompletionConstants.MESSAGE_INDENT,Messages.getString("BuiltInShellCommand.locationArg_noContextWithThisName", path)); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	/**
	 * Validates whether the supplied object type is a valid child for the supplied context.
	 * If invalid an error message is printed out.
	 * @param objType the object type
	 * @param context the workspace context
	 * @return 'true' if the child type is valid for the context, 'false' if not.
	 * @exception Exception the exception
	 */
	public boolean validateChildType(String objType, WorkspaceContext context) throws Exception {
		List<String> allowableChildTypes = context.getAllowableChildTypes();

		if(!allowableChildTypes.contains(objType.toLowerCase())) {
            print(CompletionConstants.MESSAGE_INDENT,Messages.getString("BuiltInShellCommand.typeArg_childTypeNotAllowed", objType, context.getFullName())); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	/**
	 * Validates whether the supplied property is a valid property for the supplied context.
	 * If invalid an error message is printed out.
	 * @param propName the property name
	 * @param context the workspace context
	 * @param printMessage specify whether output message printed
	 * @return 'true' if the property is valid for the context, 'false' if not.
	 * @exception Exception the exception
	 */
    public boolean validateProperty( final String propName,
                                     final WorkspaceContext context,
                                     final boolean printMessage) throws Exception {
        if ( !StringUtils.isEmpty( propName ) ) {
            final List< String > propNames = context.getProperties();

            if ( propNames.contains( propName )
                 || ( !isShowingPropertyNamePrefixes() && propNames.contains( attachPrefix( context, propName ) ) ) ) {
                return true;
            }
            
            if(printMessage) {
            	print( CompletionConstants.MESSAGE_INDENT,
            			Messages.getString( "BuiltInShellCommand.propertyArg_noPropertyWithThisName", propName ) ); //$NON-NLS-1$
            }
        }

        return false;
	}

	/**
	 * Validates whether the supplied property value is valid for the property
	 * If invalid an error message is printed out.
	 * @param propName the property name
	 * @param propValue the property value
	 * @param context the workspace context
	 * @return 'true' if the property is valid for the context, 'false' if not.
	 */
	public boolean validatePropertyValue(String propName, String propValue, WorkspaceContext context) {
		// TODO: add logic to test
		return true;
	}

	/**
	 * Validate whether the supplied propName is valid for the supplied context.  If invalid, a message is printed out.
	 * @param context the context
	 * @param propName the property name
	 * @return 'true' if valid, 'false' if not.
	 * @throws Exception exception if problem getting the value.
	 */
	public boolean validatePropertyName(WorkspaceContext context, String propName) throws Exception {
		final boolean exists = context.getProperties().contains( propName );

        if ( !exists ) {
            print(CompletionConstants.MESSAGE_INDENT,Messages.getString("BuiltInShellCommand.propertyArg_noPropertyWithThisName", propName)); //$NON-NLS-1$
			return false;
		}
		return true;
	}

    /**
     * Updates the candidates for tab completion, given the currentContext and path
     * @param candidates the candidates list
     * @param currentContext the current context
     * @param includeGoUp if 'true' the '..' option is included
     * @param lastArgument the last arg
     * @throws Exception the exception
     */
    public void updateTabCompleteCandidatesForPath(List<CharSequence> candidates, WorkspaceContext currentContext, boolean includeGoUp, String lastArgument) throws Exception {
    	// List of potentials completions
    	List<String> potentialsList = new ArrayList<String>();
    	// Only offer '..' if below the root
    	if( (currentContext.getType()!=WorkspaceStatus.WORKSPACE_TYPE) && includeGoUp ) {
    		potentialsList.add(StringConstants.DOT_DOT);
    	}

    	// --------------------------------------------------------------
    	// No arg - offer children relative current context.
    	// --------------------------------------------------------------
    	if(lastArgument==null) {
    		List<WorkspaceContext> children = currentContext.getChildren();
    		for(WorkspaceContext wsContext : children) {
    			potentialsList.add(wsContext.getName()+ContextUtils.PATH_SEPARATOR);
    		}
    		candidates.addAll(potentialsList);
    		// --------------------------------------------------------------
    		// One arg - determine the completion options for it.
    		// --------------------------------------------------------------
    	} else {
    		// --------------------------------------------
    		// Absolute Path Arg handling
    		// --------------------------------------------
    		if( lastArgument.startsWith(ContextUtils.PATH_SEPARATOR) ) {
    			// If not the full absolute root, then provide it
    			if(!ContextUtils.isAbsolutePath(lastArgument)) {
    				potentialsList.add(ContextUtils.PATH_SEPARATOR+WorkspaceContext.WORKSPACE_ROOT_DISPLAY_NAME+ContextUtils.PATH_SEPARATOR);
    				updateCandidates(candidates,potentialsList,lastArgument);
    		    // Starts with correct root - provide next option
    			} else {
    				String relativePath = ContextUtils.convertAbsolutePathToRootRelative(lastArgument);
    				WorkspaceContext deepestMatchingContext = ContextUtils.getDeepestMatchingContextRelative(getWorkspaceStatus().getWorkspaceContext(), relativePath);

    				// Get children of deepest context match to form potentialsList
    				List<WorkspaceContext> children = deepestMatchingContext.getChildren();
    				if(!children.isEmpty()) {
    					// Get all children as potentials
    					for(WorkspaceContext childContext : children) {
    						String absolutePath = childContext.getFullName();
    						potentialsList.add(absolutePath+ContextUtils.PATH_SEPARATOR);
    					}
    				} else {
    					String absolutePath = deepestMatchingContext.getFullName();
    					potentialsList.add(absolutePath+ContextUtils.PATH_SEPARATOR);
    				}
    				updateCandidates(candidates, potentialsList, lastArgument);
    			}
    			// -------------------------------------------
    			// Relative Path Arg handling
    			// -------------------------------------------
    		} else {
    			// Deepest matching context for relative path
    			WorkspaceContext deepestMatchingContext = ContextUtils.getDeepestMatchingContextRelative(currentContext, lastArgument);

    			// Get children of deepest context match to form potentialsList
    			List<WorkspaceContext> children = deepestMatchingContext.getChildren();
    			if(!children.isEmpty()) {
    				// Get all children as potentials
    				for(WorkspaceContext childContext : children) {
    					String absolutePath = childContext.getFullName();
    					String relativePath = ContextUtils.convertAbsolutePathToRelative(currentContext, absolutePath);
    					potentialsList.add(relativePath+ContextUtils.PATH_SEPARATOR);
    				}
    			} else {
    				String absolutePath = deepestMatchingContext.getFullName();
    				String relativePath = ContextUtils.convertAbsolutePathToRelative(currentContext, absolutePath);
    				potentialsList.add(relativePath+ContextUtils.PATH_SEPARATOR);
    			}
    			updateCandidates(candidates, potentialsList, lastArgument);
    		}

    	}
    }

    /**
     * Updates the candidates for tab completion, given the context and property Arg
     * @param candidates the candidates list
     * @param context the context
     * @param propArg the propName for completion
     * @throws Exception the exception
     */
    public void updateTabCompleteCandidatesForProperty(List<CharSequence> candidates, WorkspaceContext context, String propArg) throws Exception {
		// List of potentials completions
		List<String> potentials = null;

		// Context properties
		final List<String> propNames = context.getProperties();  // All properties

        if ( isShowingPropertyNamePrefixes() ) {
            potentials = propNames;
        } else {
            potentials = new ArrayList<>( propNames.size() );

            // strip off prefix
            for ( final String name : propNames ) {
                potentials.add( removePrefix( name ) );
            }
        }

        Collections.sort( potentials );

        if ( StringUtils.isEmpty( propArg ) ) {
            candidates.addAll( potentials );
        } else {
            updateCandidates( candidates, potentials, propArg );
        }
    }

    /**
     * Adds the valid items from the completionList to the candidates.  They are added to the candidates if they start
     * with 'lastArg'
     * @param candidates the candidates
     * @param completionList possibilities before filtering based on last arg
     * @param lastArg the commandline arg
     */
    private void updateCandidates(List<CharSequence> candidates, List<String> completionList, String lastArg) {
        updateCandidates( candidates, completionList, lastArg, true );
    }

    private void updateCandidates( final List< CharSequence > candidates,
                                   final List< String > completionList,
                                   final String lastArg,
                                   final boolean caseSensitive ) {
        for ( final String item : completionList ) {
            if (caseSensitive) {
                if ( item.startsWith( lastArg )) {
                    candidates.add( item );
                }
            } else if ( item.toUpperCase().startsWith( lastArg.toUpperCase() ) ) {
                candidates.add( item );
            }
        }
    }

}
