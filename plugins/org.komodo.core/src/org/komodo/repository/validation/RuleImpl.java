/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.komodo.repository.validation;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.komodo.core.KomodoLexicon;
import org.komodo.repository.Messages;
import org.komodo.repository.ObjectImpl;
import org.komodo.spi.KException;
import org.komodo.spi.constants.StringConstants;
import org.komodo.spi.outcome.Outcome.Level;
import org.komodo.spi.outcome.OutcomeFactory.OutcomeImpl;
import org.komodo.spi.repository.KomodoObject;
import org.komodo.spi.repository.Property;
import org.komodo.spi.repository.PropertyValueType;
import org.komodo.spi.repository.Repository;
import org.komodo.spi.repository.Repository.UnitOfWork;
import org.komodo.spi.repository.Repository.UnitOfWork.State;
import org.komodo.spi.repository.validation.Result;
import org.komodo.spi.repository.validation.Rule;
import org.komodo.utils.ArgCheck;
import org.komodo.utils.StringUtils;

/**
 * An implementation of a {@link Rule validation rule}.
 */
public class RuleImpl extends ObjectImpl implements Rule {

    class ResultImpl extends OutcomeImpl implements Result {

        private final String path;
        private final String ruleId;
        private final long timeCreated;

        ResultImpl( final String nodePath,
                    final String evaluationRuleId,
                    final Level outcomeLevel,
                    final String outcomeMessage ) {
            this.path = nodePath;
            this.ruleId = evaluationRuleId;
            this.timeCreated = System.currentTimeMillis();
            setMessage( outcomeMessage );
            setLevel( outcomeLevel );
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.spi.repository.validation.Result#getPath()
         */
        @Override
        public String getPath() {
            return this.path;
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.spi.repository.validation.Result#getRuleId()
         */
        @Override
        public String getRuleId() {
            return this.ruleId;
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.spi.repository.validation.Result#getTimestamp()
         */
        @Override
        public long getTimestamp() {
            return this.timeCreated;
        }

    }

    private static String getLocaleCode( final boolean includeCountry,
                                         final boolean includeVariant ) {
        final Locale locale = Locale.getDefault();
        final StringBuilder result = new StringBuilder( locale.getLanguage() );

        if ( includeCountry ) {
            final String country = locale.getCountry();

            if ( !StringUtils.isBlank( country ) ) {
                result.append( '_' ).append( country );

                if ( includeVariant ) {
                    final String variant = locale.getVariant();

                    if ( !StringUtils.isBlank( variant ) ) {
                        result.append( '_' ).append( variant );
                    }
                }

            }
        }

        return result.toString();
    }

    /**
     * Constructs a validation rule.
     *
     * @param uow
     *        the transaction (can be <code>null</code> if update should be automatically committed)
     * @param repository
     *        the repository where the relational object exists (cannot be <code>null</code>)
     * @param path
     *        the path (cannot be empty but assumed to be a path to a rule)
     * @throws KException
     *         if an error occurs
     */
    public RuleImpl( final UnitOfWork uow,
                     final Repository repository,
                     final String path ) throws KException {
        super( repository, path, 0 );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.validation.Rule#evaluate(org.komodo.spi.repository.Repository.UnitOfWork,
     *      org.komodo.spi.repository.KomodoObject)
     */
    @Override
    public Result evaluate( final UnitOfWork transaction,
                            final KomodoObject kobject ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$
        ArgCheck.isNotNull( kobject, "kobject" ); //$NON-NLS-1$

        if ( !isEnabled( transaction ) ) {
            throw new KException( Messages.getString( Messages.Validation.ATTEMPT_TO_EVALUATE_DISABLED_RULE, getAbsolutePath() ) );
        }

        try {
            final RuleType ruleType = getRuleType( transaction );

            switch ( getValidationType( transaction ) ) {
                case NODE:
                    return evaluateNodeRule( transaction, kobject, ruleType );
                case PROPERTY:
                    return evaluatePropertyRule( transaction, kobject, ruleType );
                case CHILD:
                    return evaluateChildRule( transaction, kobject, ruleType );
                default:
                    // need to add new validation type
                    throw new RuntimeException( "Unexpected validation type of '" + getValidationType( transaction ) + '\'' ); //$NON-NLS-1$
            }
        } catch ( final Exception e ) {
            if ( e instanceof KException ) {
                throw ( KException )e;
            }

            throw new KException( e );
        }
    }

    private Result evaluateChildRule( final UnitOfWork transaction,
                                      final KomodoObject kobject,
                                      final RuleType ruleType ) throws KException {
        final Property childTypeProp = getProperty( transaction, KomodoLexicon.Rule.JCR_NAME );
        assert ( childTypeProp != null );

        final String childType = childTypeProp.getStringValue( transaction );
        String errorMsg = null;
        String[] args = null;

        switch ( ruleType ) {
            case REQUIRED: {
                if ( kobject.getChildrenOfType( transaction, childType ).length == 0 ) {
                    args = new String[] { childType, kobject.getName( transaction ), kobject.getAbsolutePath() };
                    errorMsg = getMessage( transaction, MessageKey.CHILD_OF_REQUIRED_TYPE_NOT_FOUND.name(), args );

                    // if no error message found use default message
                    if ( StringUtils.isBlank( errorMsg ) ) {
                        errorMsg = Messages.getString( Messages.Validation.CHILD_OF_REQUIRED_TYPE_NOT_FOUND, ( Object[] )args );
                    }
                }

                break;
            }
            case NUMBER: {
                final Integer childCount = kobject.getChildrenOfType( transaction, childType ).length;
                boolean minChecked = false;
                boolean maxChecked = false;

                try {
                    { // check min value
                        final Property minValueProp = getProperty( transaction, KomodoLexicon.Rule.MIN_VALUE );

                        if ( minValueProp != null ) {
                            minChecked = true;
                            boolean inclusive = true;
                            final Property minInclusiveProp = getProperty( transaction, KomodoLexicon.Rule.MIN_VALUE_INCLUSIVE );

                            if ( minInclusiveProp != null ) {
                                inclusive = minInclusiveProp.getBooleanValue( transaction );
                            }

                            final String minString = minValueProp.getStringValue( transaction );
                            final Number minValue = NumberFormat.getInstance().parse( minString );
                            final int result = Double.compare( childCount.doubleValue(), minValue.doubleValue() );

                            if ( ( inclusive && ( result < 0 ) ) || ( !inclusive && ( result <= 0 ) ) ) {
                                args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(),
                                    childCount.toString(), childType, minString };
                                errorMsg = getMessage( transaction, MessageKey.CHILD_COUNT_BELOW_MIN_VALUE.name(), args );

                                // if no error message found use default message
                                if ( StringUtils.isBlank( errorMsg ) ) {
                                    errorMsg = Messages.getString( Messages.Validation.CHILD_COUNT_BELOW_MIN_VALUE,
                                                                   ( Object[] )args );
                                }
                            }
                        }
                    }

                    if ( StringUtils.isBlank( errorMsg ) ) {
                        { // check max value
                            final Property maxValueProp = getProperty( transaction, KomodoLexicon.Rule.MAX_VALUE );

                            if ( maxValueProp != null ) {
                                maxChecked = true;
                                boolean inclusive = true;
                                final Property maxInclusiveProp = getProperty( transaction,
                                                                               KomodoLexicon.Rule.MAX_VALUE_INCLUSIVE );

                                if ( maxInclusiveProp != null ) {
                                    inclusive = maxInclusiveProp.getBooleanValue( transaction );
                                }

                                final String maxString = maxValueProp.getStringValue( transaction );
                                final Number maxValue = NumberFormat.getInstance().parse( maxString );
                                final int result = Double.compare( childCount.doubleValue(), maxValue.doubleValue() );

                                if ( ( inclusive && ( result > 0 ) ) || ( !inclusive && ( result >= 0 ) ) ) {
                                    args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(),
                                        childCount.toString(), childType, maxString };
                                    errorMsg = getMessage( transaction, MessageKey.CHILD_COUNT_ABOVE_MAX_VALUE.name(), args );

                                    // if no error message found use default message
                                    if ( StringUtils.isBlank( errorMsg ) ) {
                                        errorMsg = Messages.getString( Messages.Validation.CHILD_COUNT_ABOVE_MAX_VALUE,
                                                                       ( Object[] )args );
                                    }
                                }
                            }
                        }
                    }

                    if ( StringUtils.isBlank( errorMsg ) && !minChecked && !maxChecked ) {
                        args = new String[] { getName( transaction ) };
                        errorMsg = getMessage( transaction, MessageKey.NUMBER_RULE_HAS_NO_VALUES.name(), args );

                        // if no error message found use default message
                        if ( StringUtils.isBlank( errorMsg ) ) {
                            errorMsg = Messages.getString( Messages.Validation.NUMBER_RULE_HAS_NO_VALUES, ( Object[] )args );
                        }
                    }
                } catch ( final ParseException ex ) {
                    // not a valid number rule and should be caught by XSD validation
                    args = new String[] { getName( transaction ) };
                    errorMsg = getMessage( transaction, MessageKey.NUMBER_RULE_NON_NUMERIC_VALUES.name(), args );

                    // if no error message found use default message
                    if ( StringUtils.isBlank( errorMsg ) ) {
                        errorMsg = Messages.getString( Messages.Validation.NUMBER_RULE_NON_NUMERIC_VALUES, ( Object[] )args );
                    }
                }

                break;
            }
            case RELATIONSHIP: {
                // must have at least one child of this type to invoke this rule
                if ( kobject.getChildrenOfType( transaction, childType ).length == 0 ) {
                    break;
                }

                { // props exist
                    final Property propExistsProp = getProperty( transaction, KomodoLexicon.Rule.PROP_EXISTS );

                    if ( propExistsProp != null ) {
                        for ( final String prop : propExistsProp.getStringValues( transaction ) ) {
                            if ( !kobject.hasProperty( transaction, prop ) ) {
                                args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(), childType, prop };
                                errorMsg = getMessage( transaction,
                                                       MessageKey.RELATIONSHIP_RULE_REQUIRED_PROPERTY_NOT_FOUND.name(),
                                                       args );

                                // if no error message found use default message
                                if ( StringUtils.isBlank( errorMsg ) ) {
                                    errorMsg = Messages.getString( Messages.Validation.RELATIONSHIP_RULE_REQUIRED_PROPERTY_NOT_FOUND,
                                                                   ( Object[] )args );
                                }

                                break;
                            }
                        }
                    }
                }

                if ( !StringUtils.isBlank( errorMsg ) ) {
                    break;
                }

                { // props absent
                    final Property propAbsentProp = getProperty( transaction, KomodoLexicon.Rule.PROP_ABSENT );

                    if ( propAbsentProp != null ) {
                        for ( final String prop : propAbsentProp.getStringValues( transaction ) ) {
                            if ( kobject.hasProperty( transaction, prop ) ) {
                                args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(), childType, prop };
                                errorMsg = getMessage( transaction,
                                                       MessageKey.RELATIONSHIP_RULE_ABSENT_PROPERTY_FOUND.name(),
                                                       args );

                                // if no error message found use default message
                                if ( StringUtils.isBlank( errorMsg ) ) {
                                    errorMsg = Messages.getString( Messages.Validation.RELATIONSHIP_RULE_ABSENT_PROPERTY_FOUND,
                                                                   ( Object[] )args );
                                }

                                break;
                            }
                        }
                    }
                }

                if ( StringUtils.isBlank( errorMsg ) ) {
                    break;
                }

                { // children exist
                    final Property childExistsProp = getProperty( transaction, KomodoLexicon.Rule.CHILD_EXISTS );

                    if ( childExistsProp != null ) {
                        for ( final String kidType : childExistsProp.getStringValues( transaction ) ) {
                            if ( kobject.getChildrenOfType( transaction, kidType ).length == 0 ) {
                                args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(), childType,
                                    kidType };
                                errorMsg = getMessage( transaction,
                                                       MessageKey.RELATIONSHIP_RULE_REQUIRED_CHILD_NOT_FOUND.name(),
                                                       args );

                                // if no error message found use default message
                                if ( StringUtils.isBlank( errorMsg ) ) {
                                    errorMsg = Messages.getString( Messages.Validation.RELATIONSHIP_RULE_REQUIRED_CHILD_NOT_FOUND,
                                                                   ( Object[] )args );
                                }

                                break;
                            }
                        }
                    }
                }

                if ( StringUtils.isBlank( errorMsg ) ) {
                    break;
                }

                { // children absent
                    final Property childAbsentProp = getProperty( transaction, KomodoLexicon.Rule.CHILD_ABSENT );

                    if ( childAbsentProp != null ) {
                        for ( final String kidType : childAbsentProp.getStringValues( transaction ) ) {
                            if ( kobject.getChildrenOfType( transaction, kidType ).length > 0 ) {
                                args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(), childType,
                                    kidType };
                                errorMsg = getMessage( transaction, MessageKey.RELATIONSHIP_RULE_ABSENT_CHILD_FOUND.name(), args );

                                // if no error message found use default message
                                if ( StringUtils.isBlank( errorMsg ) ) {
                                    errorMsg = Messages.getString( Messages.Validation.RELATIONSHIP_RULE_ABSENT_CHILD_FOUND,
                                                                   ( Object[] )args );
                                }

                                break;
                            }
                        }
                    }
                }

                break;
            }
            case SAME_NAME_SIBLING: {
                final Set< String > names = new HashSet<>();

                for ( final KomodoObject kid : kobject.getChildrenOfType( transaction, childType ) ) {
                    final String name = kid.getName( transaction );

                    if ( !names.add( name ) ) {
                        args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(), name, childType };
                        errorMsg = getMessage( transaction, MessageKey.RELATIONSHIP_RULE_SNS_FOUND.name(), args );

                        // if no error message found use default message
                        if ( StringUtils.isBlank( errorMsg ) ) {
                            errorMsg = Messages.getString( Messages.Validation.RELATIONSHIP_RULE_SNS_FOUND, ( Object[] )args );
                        }

                        break;
                    }
                }

                break;
            }
            case PATTERN:
                assert false; // not a valid rule and should be caught by XSD validation
                break;
            default:
                assert false; // need to add a new case statement
                break;
        }

        if ( StringUtils.isBlank( errorMsg ) ) {
            return new ResultImpl( kobject.getAbsolutePath(), getName( transaction ), Level.OK, StringConstants.EMPTY_STRING );
        }

        assert ( !StringUtils.isBlank( errorMsg ) );
        return new ResultImpl( kobject.getAbsolutePath(), getName( transaction ), getSeverity( transaction ), errorMsg );
    }

    private Result evaluateNodeRule( final UnitOfWork transaction,
                                     final KomodoObject kobject,
                                     final RuleType ruleType ) throws KException {
        String errorMsg = null;
        String[] args = null;

        switch ( ruleType ) {
            case PATTERN: {
                final Property patternProp = getProperty( transaction, KomodoLexicon.Rule.PATTERN );
                assert ( patternProp != null );

                final String name = kobject.getName( transaction );

                if ( !name.matches( patternProp.getStringValue( transaction ) ) ) {
                    args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath() };
                    errorMsg = getMessage( transaction, MessageKey.PATTERN_RULE_INVALID_NODE_NAME.name(), args );

                    // if no error message found use default message
                    if ( StringUtils.isBlank( errorMsg ) ) {
                        errorMsg = Messages.getString( Messages.Validation.PATTERN_RULE_INVALID_NODE_NAME, ( Object[] )args );
                    }
                }

                break;
            }
            case REQUIRED:
            case NUMBER:
            case RELATIONSHIP:
            case SAME_NAME_SIBLING:
                assert false; // not a valid rule and should be caught be XSD validation
                break;
            default:
                assert false; // need to add a new case statement
                break;
        }

        if ( StringUtils.isBlank( errorMsg ) ) {
            return new ResultImpl( kobject.getAbsolutePath(), getName( transaction ), Level.OK, StringConstants.EMPTY_STRING );
        }

        assert ( !StringUtils.isBlank( errorMsg ) );
        return new ResultImpl( kobject.getAbsolutePath(), getName( transaction ), getSeverity( transaction ), errorMsg );
    }

    private Result evaluatePropertyRule( final UnitOfWork transaction,
                                         final KomodoObject kobject,
                                         final RuleType ruleType ) throws KException {
        final Property jcrNameProp = getProperty( transaction, KomodoLexicon.Rule.JCR_NAME );
        assert ( jcrNameProp != null );

        final String propName = jcrNameProp.getStringValue( transaction );
        final boolean exists = kobject.hasProperty( transaction, propName );
        String errorMsg = null;
        String[] args = null;

        switch ( ruleType ) {
            case REQUIRED: {
                if ( !exists ) {
                    args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(), propName };
                    errorMsg = getMessage( transaction, MessageKey.REQUIRED_PROPERTY_NOT_FOUND.name(), args );

                    // if no error message found use default message
                    if ( StringUtils.isBlank( errorMsg ) ) {
                        errorMsg = Messages.getString( Messages.Validation.REQUIRED_PROPERTY_NOT_FOUND, ( Object[] )args );
                    }
                }

                break;
            }
            case PATTERN: {
                // property must exist
                if ( !exists ) {
                    break;
                }

                final Property patternProp = getProperty( transaction, KomodoLexicon.Rule.PATTERN );
                assert ( patternProp != null );

                final String value = kobject.getProperty( transaction, propName ).getStringValue( transaction );

                if ( !value.matches( patternProp.getStringValue( transaction ) ) ) {
                    args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(), propName };
                    errorMsg = getMessage( transaction, MessageKey.PATTERN_RULE_INVALID_PROPERTY_VALUE.name(), args );

                    // if no error message found use default message
                    if ( StringUtils.isBlank( errorMsg ) ) {
                        errorMsg = Messages.getString( Messages.Validation.PATTERN_RULE_INVALID_PROPERTY_VALUE, ( Object[] )args );
                    }
                }

                break;
            }
            case NUMBER: {
                // property must exist
                if ( !exists ) {
                    break;
                }

                boolean minChecked = false;
                boolean maxChecked = false;
                final String valueString = kobject.getProperty( transaction, propName ).getStringValue( transaction );

                if ( StringUtils.isNumber( valueString ) ) {
                    try {
                        final Number value = NumberFormat.getInstance().parse( valueString );

                        { // check min value
                            final Property minValueProp = getProperty( transaction, KomodoLexicon.Rule.MIN_VALUE );

                            if ( minValueProp != null ) {
                                minChecked = true;
                                boolean inclusive = true;
                                final Property minInclusiveProp = getProperty( transaction,
                                                                               KomodoLexicon.Rule.MIN_VALUE_INCLUSIVE );

                                if ( minInclusiveProp != null ) {
                                    inclusive = minInclusiveProp.getBooleanValue( transaction );
                                }

                                final String minString = minValueProp.getStringValue( transaction );
                                final Number minValue = NumberFormat.getInstance().parse( minString );
                                final int result = Double.compare( value.doubleValue(), minValue.doubleValue() );

                                if ( ( inclusive && ( result < 0 ) ) || ( !inclusive && ( result <= 0 ) ) ) {
                                    args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(), propName,
                                        valueString, minString };
                                    errorMsg = getMessage( transaction,
                                                           MessageKey.PROPERTY_RULE_VALUE_BELOW_MIN_VALUE.name(),
                                                           args );

                                    // if no error message found use default message
                                    if ( StringUtils.isBlank( errorMsg ) ) {
                                        errorMsg = Messages.getString( Messages.Validation.PROPERTY_RULE_VALUE_BELOW_MIN_VALUE,
                                                                       ( Object[] )args );
                                    }
                                }
                            }
                        }

                        if ( StringUtils.isBlank( errorMsg ) ) {
                            { // check max value
                                final Property maxValueProp = getProperty( transaction, KomodoLexicon.Rule.MAX_VALUE );

                                if ( maxValueProp != null ) {
                                    maxChecked = true;
                                    boolean inclusive = true;
                                    final Property maxInclusiveProp = getProperty( transaction,
                                                                                   KomodoLexicon.Rule.MAX_VALUE_INCLUSIVE );

                                    if ( maxInclusiveProp != null ) {
                                        inclusive = maxInclusiveProp.getBooleanValue( transaction );
                                    }

                                    final String maxString = maxValueProp.getStringValue( transaction );
                                    final Number maxValue = NumberFormat.getInstance().parse( maxString );
                                    final int result = Double.compare( value.doubleValue(), maxValue.doubleValue() );

                                    if ( ( inclusive && ( result > 0 ) ) || ( !inclusive && ( result >= 0 ) ) ) {
                                        args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(),
                                            propName, valueString, maxString };
                                        errorMsg = getMessage( transaction,
                                                               MessageKey.PROPERTY_RULE_VALUE_ABOVE_MAX_VALUE.name(),
                                                               args );

                                        // if no error message found use default message
                                        if ( StringUtils.isBlank( errorMsg ) ) {
                                            errorMsg = Messages.getString( Messages.Validation.PROPERTY_RULE_VALUE_ABOVE_MAX_VALUE,
                                                                           ( Object[] )args );
                                        }
                                    }
                                }
                            }
                        }

                        if ( StringUtils.isBlank( errorMsg ) && !minChecked && !maxChecked ) {
                            args = new String[] { getName( transaction ) };
                            errorMsg = getMessage( transaction, MessageKey.NUMBER_RULE_HAS_NO_VALUES.name(), args );

                            // if no error message found use default message
                            if ( StringUtils.isBlank( errorMsg ) ) {
                                errorMsg = Messages.getString( Messages.Validation.NUMBER_RULE_HAS_NO_VALUES, ( Object[] )args );
                            }
                        }
                    } catch ( final ParseException ex ) {
                        // not a valid number rule and should be caught by XSD validation
                        args = new String[] { getName( transaction ) };
                        errorMsg = getMessage( transaction, MessageKey.NUMBER_RULE_NON_NUMERIC_VALUES.name(), args );

                        // if no error message found use default message
                        if ( StringUtils.isBlank( errorMsg ) ) {
                            errorMsg = Messages.getString( Messages.Validation.NUMBER_RULE_NON_NUMERIC_VALUES, ( Object[] )args );
                        }
                    }
                }

                break;
            }
            case RELATIONSHIP: {
                if ( !exists ) {
                    break;
                }

                { // props exist
                    final Property propExistsProp = getProperty( transaction, KomodoLexicon.Rule.PROP_EXISTS );

                    if ( propExistsProp != null ) {
                        for ( final String prop : propExistsProp.getStringValues( transaction ) ) {
                            if ( !kobject.hasProperty( transaction, prop ) ) {
                                args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(), propName, prop };
                                errorMsg = getMessage( transaction,
                                                       MessageKey.PROPERTY_RULE_REQUIRED_PROPERTY_NOT_FOUND.name(),
                                                       args );

                                // if no error message found use default message
                                if ( StringUtils.isBlank( errorMsg ) ) {
                                    errorMsg = Messages.getString( Messages.Validation.PROPERTY_RULE_REQUIRED_PROPERTY_NOT_FOUND,
                                                                   ( Object[] )args );
                                }

                                break;
                            }
                        }
                    }
                }

                if ( !StringUtils.isBlank( errorMsg ) ) {
                    break;
                }

                { // props absent
                    final Property propAbsentProp = getProperty( transaction, KomodoLexicon.Rule.PROP_ABSENT );

                    if ( propAbsentProp != null ) {
                        for ( final String prop : propAbsentProp.getStringValues( transaction ) ) {
                            if ( kobject.hasProperty( transaction, prop ) ) {
                                args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(), propName, prop };
                                errorMsg = getMessage( transaction, MessageKey.PROPERTY_RULE_ABSENT_PROPERTY_FOUND.name(), args );

                                // if no error message found use default message
                                if ( StringUtils.isBlank( errorMsg ) ) {
                                    errorMsg = Messages.getString( Messages.Validation.PROPERTY_RULE_ABSENT_PROPERTY_FOUND,
                                                                   ( Object[] )args );
                                }

                                break;
                            }
                        }
                    }
                }

                if ( !StringUtils.isBlank( errorMsg ) ) {
                    break;
                }

                { // children exist
                    final Property childExistsProp = getProperty( transaction, KomodoLexicon.Rule.CHILD_EXISTS );

                    if ( childExistsProp != null ) {
                        for ( final String childType : childExistsProp.getStringValues( transaction ) ) {
                            if ( kobject.getChildrenOfType( transaction, childType ).length == 0 ) {
                                args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(), propName,
                                    childType };
                                errorMsg = getMessage( transaction,
                                                       MessageKey.RELATIONSHIP_RULE_REQUIRED_CHILD_NOT_FOUND.name(),
                                                       args );

                                // if no error message found use default message
                                if ( StringUtils.isBlank( errorMsg ) ) {
                                    errorMsg = Messages.getString( Messages.Validation.RELATIONSHIP_RULE_REQUIRED_CHILD_NOT_FOUND,
                                                                   ( Object[] )args );
                                }

                                break;
                            }
                        }
                    }
                }

                if ( !StringUtils.isBlank( errorMsg ) ) {
                    break;
                }

                { // children absent
                    final Property childAbsentProp = getProperty( transaction, KomodoLexicon.Rule.CHILD_ABSENT );

                    if ( childAbsentProp != null ) {
                        for ( final String childType : childAbsentProp.getStringValues( transaction ) ) {
                            if ( kobject.getChildrenOfType( transaction, childType ).length > 0 ) {
                                args = new String[] { kobject.getName( transaction ), kobject.getAbsolutePath(), propName,
                                    childType };
                                errorMsg = getMessage( transaction, MessageKey.PROPERTY_RULE_ABSENT_CHILD_FOUND.name(), args );

                                // if no error message found use default message
                                if ( StringUtils.isBlank( errorMsg ) ) {
                                    errorMsg = Messages.getString( Messages.Validation.PROPERTY_RULE_ABSENT_CHILD_FOUND,
                                                                   ( Object[] )args );
                                }

                                break;
                            }
                        }
                    }
                }

                break;
            }
            case SAME_NAME_SIBLING:
                assert false; // not a valid rule and should be caught by XSD validation
                break;
            default:
                assert false; // need to add a new case statement
                break;
        }

        if ( StringUtils.isBlank( errorMsg ) ) {
            return new ResultImpl( kobject.getAbsolutePath(), getName( transaction ), Level.OK, StringConstants.EMPTY_STRING );
        }

        return new ResultImpl( kobject.getAbsolutePath(), getName( transaction ), getSeverity( transaction ), errorMsg );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.validation.Rule#getDescription(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getDescription( final UnitOfWork transaction ) throws KException {
        return getMessage( transaction, MessageKey.DESCRIPTION.name() );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.validation.Rule#getJcrName(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getJcrName( final UnitOfWork transaction ) throws KException {
        return getObjectProperty( transaction, PropertyValueType.STRING, "getJcrName", KomodoLexicon.Rule.JCR_NAME ); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.validation.Rule#getMessage(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String, java.lang.String[])
     */
    @Override
    public String getMessage( final UnitOfWork transaction,
                              final String key,
                              final String... args ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        KomodoObject localizedText = null;

        if ( hasChild( transaction, KomodoLexicon.Rule.MESSAGES ) ) {
            final KomodoObject messages = getChild( transaction,
                                                    KomodoLexicon.Rule.MESSAGES,
                                                    KomodoLexicon.Rule.LOCALIZED_MESSAGE_GROUPING );

            if ( messages.hasChild( transaction, key, KomodoLexicon.Rule.LOCALIZED_MESSAGE ) ) {
                final KomodoObject message = messages.getChild( transaction, key, KomodoLexicon.Rule.LOCALIZED_MESSAGE );

                if ( message.hasChild( transaction, getLocaleCode( true, true ), KomodoLexicon.Rule.LOCALIZED_TEXT_TYPE ) ) {
                    localizedText = message.getChild( transaction,
                                                      getLocaleCode( true, true ),
                                                      KomodoLexicon.Rule.LOCALIZED_TEXT_TYPE );
                } else if ( message.hasChild( transaction, getLocaleCode( true, false ), KomodoLexicon.Rule.LOCALIZED_TEXT_TYPE ) ) {
                    localizedText = message.getChild( transaction,
                                                      getLocaleCode( true, false ),
                                                      KomodoLexicon.Rule.LOCALIZED_TEXT_TYPE );
                } else if ( message.hasChild( transaction, getLocaleCode( false, false ), KomodoLexicon.Rule.LOCALIZED_TEXT_TYPE ) ) {
                    localizedText = message.getChild( transaction,
                                                      getLocaleCode( false, false ),
                                                      KomodoLexicon.Rule.LOCALIZED_TEXT_TYPE );
                }
            }
        }

        if ( localizedText == null ) {
            return Messages.getString( Messages.Validation.RULE_MESSAGE_NOT_FOUND, key, getAbsolutePath() );
        }

        final Property prop = localizedText.getProperty( transaction, KomodoLexicon.Rule.LOCALIZED_TEXT );
        return prop.getStringValue( transaction );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.validation.Rule#getNodeType(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getNodeType( final UnitOfWork transaction ) throws KException {
        return getObjectProperty( transaction, PropertyValueType.STRING, "getNodeType", KomodoLexicon.Rule.NODE_TYPE ); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.validation.Rule#getRuleType(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public RuleType getRuleType( final UnitOfWork transaction ) throws KException {
        final String value = getObjectProperty( transaction,
                                                PropertyValueType.STRING,
                                                "getRuleType", KomodoLexicon.Rule.RULE_TYPE ); //$NON-NLS-1$

        try {
            final RuleType result = RuleType.valueOf( value );
            return result;
        } catch ( final Exception e ) {
            throw new KException( e );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.validation.Rule#getSeverity(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public Level getSeverity( final UnitOfWork transaction ) throws KException {
        final String value = getObjectProperty( transaction, PropertyValueType.STRING, "getSeverity", KomodoLexicon.Rule.SEVERITY ); //$NON-NLS-1$

        try {
            final Level result = Level.valueOf( value );
            return result;
        } catch ( final Exception e ) {
            return Level.ERROR;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.validation.Rule#getValidationType(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public ValidationType getValidationType( final UnitOfWork transaction ) throws KException {
        final String value = getObjectProperty( transaction, PropertyValueType.STRING, "getValidationType", //$NON-NLS-1$
                                                KomodoLexicon.Rule.VALIDATION_TYPE );

        try {
            final ValidationType result = ValidationType.valueOf( value );
            return result;
        } catch ( final Exception e ) {
            throw new KException( e );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.validation.Rule#isEnabled(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public boolean isEnabled( final UnitOfWork transaction ) throws KException {
        final Boolean enabled = getObjectProperty( transaction,
                                                   PropertyValueType.BOOLEAN,
                                                   "isEnabled", KomodoLexicon.Rule.ENABLED ); //$NON-NLS-1$
        return ( enabled == null ) ? true : enabled;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.validation.Rule#setEnabled(org.komodo.spi.repository.Repository.UnitOfWork, boolean)
     */
    @Override
    public void setEnabled( final UnitOfWork transaction,
                            final boolean newEnabled ) throws KException {
        setObjectProperty( transaction, "setEnabled", KomodoLexicon.Rule.ENABLED, newEnabled ); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.validation.Rule#setSeverity(org.komodo.spi.repository.Repository.UnitOfWork,
     *      org.komodo.spi.outcome.Outcome.Level)
     */
    @Override
    public void setSeverity( final UnitOfWork transaction,
                             final Level newLevel ) throws KException {
        setObjectProperty( transaction, "setSeverity", KomodoLexicon.Rule.ENABLED, newLevel ); //$NON-NLS-1$
    }

}
