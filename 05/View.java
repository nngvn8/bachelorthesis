/**
 * Class used as parent of all model abstractions implemented. Contains the definition for all utility functions
 *
 * Single Abstractions only need to define the buildModel() function, that describes how the abstraction exactly works.
 */
public abstract class View implements Namespace {

    protected final ViewType type;

    protected final Model model;

    protected long id;

    protected Map<String,Set<String>> stateRestriction = new HashMap<>();
    // stores the restriction used to determine the set of relevantStates
    // might be obsolete
    // implemented for the option of changing the set of relevant states at later execution time

    boolean relevantStatesAreProperSubset = false;

    protected Set<Long> relevantStates;
    // default: all states
    // states that a views will be created on (refers to \smstates)
    // all other states will receive the non grouping Symbol

    protected boolean isActive = true;
    // currently unused feature to deactivate view
    // a deactivated view stays in the database


    protected enum BinaryMode {SHOW, HIDE}
    // HIDE: Group states that have the property -> HIDE states with property in a single one
    // SHOW: DON'T group states that have the property -> SHOW states with that property

    protected BinaryMode binaryMode = BinaryMode.SHOW; // intention: filter/find/show
    // not used in every views, but only ones that are truly binary or contain a truly binary version

    protected boolean semiGrouping = true;
    // relevant when binaryMode == HIDE (otherwise irrelevant)
    // true: remaining states without property grouped
    // false: remaining states without property NOT grouped



    public void buildView() {
        try {

            // 1. View Specific Checks
            if (!viewRequirementsFulfilled() || !isActive) {
                return;
            }

            // 2.
            model.getDatabase().execute(String.format("ALTER TABLE %s ADD COLUMN %s TEXT DEFAULT %s",
                model.getStateTableName(), getCollumn(), Namespace.ENTRY_C_BLANK));


            // 3. Create views
            List<String> toExecute = groupingFunction();


            // 4. Write AP Labels to database
            model.getDatabase().executeBatch(toExecute);

        } catch(Exception e){
            throw new RuntimeException(e);
        }

    }

    protected abstract List<String> groupingFunction() throws Exception;
    // function that performs the logical part of buildView()
    // returns list of sql statements that write the assigned value to the database


    // removes this views form model.views and from the DB
    public void remove() {
        removeFromDb();
        removeFromViewList();
    }

    public void removeFromDb() {
        model.removeViewFromDbByColName(getCollumn());
    }

    public void removeFromViewList() {
        model.removeFromViews(this);
    }

    public void addToViewList() {
        model.getViews().add(this);
    }

    public void rebuildView() {
        removeFromDb();
        buildView();
    }

    protected String calcBinGroupingString(boolean hasProperty, String hasString, String hasNotString) {
        String groupingString;
        if (semiGrouping){
            switch (binaryMode) {
                case HIDE:
                    groupingString = hasProperty ? hasString : ENTRY_C_BLANK;
                    break;
                case SHOW: default:
                    groupingString = hasProperty ? ENTRY_C_BLANK : hasNotString;
                    break;
            }
        }
        else {
            groupingString = hasProperty ? hasString : hasNotString;
        }
        return groupingString;
    }
