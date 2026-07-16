# Group Decisions - V1
- If the AI needs any kind of assets in the develop phase that is not there yet, should notify the developer so the asset is provided to the AI agent.
- We will be using Hilt as our DI.
- The folders will be like this:
  - ui: the ui related files will be here. theme, navigation and view models will also be here.
    - ui/navigation: the AppNavGraph.kt will be here.
    - ui/theme: all the colors, fonts, shapes, and themes
    - ui/screens: Each screen (composables + view model) will have a folder in this directory.
    - ui/components: All the composables that are used in more than 1 screen will be here, each group of them having their own file.
    - If any file does not happen to be in any of these categories, it will be discussed where to place it (in ui, or ui/some-folder)
  - repository: All the repository related files are here. It consists of repository interface(s) and implementation(s).
  - model: All the data classes, enum classes, or data objects (or any other things like these) models which used in the ui will be stored here. 
  - di: All the di modules are stored here.
  - data: Any data-related file will be in here. The default folders in here are:
    - data/settings: Files related to app settings that are responsible for storing and restoring data will be here.
    - data/serialization: For example JsonProvider (if needed)
    - data/local: The local database files (like DAO) will be here.
      - data/local/entity: The entities (tables) used in the DAOs.
      - data/local/dao: All the DAOs will be here.
      - data/local/database: The App Database will be defined here.
      - data/local/converter: All the converters that convert any kind of data to sqlite compatible and vise versa will be here.
    - data/remote: All the networking stuff will be here.
  - util: Any function or class that is used all around the project is in here. But is is recommended not to use this folder since it makes more coupling.
- If there is a new folder or subfolder needed in the future, we will add it.
- Each feature will be developed in its own branch. Sub features will be in their own branch too and will be merged to the main feature branches.
- After each feature is done completely, it will be merged to the master branch.
- The ui specific features and the data specific features will be developed in parallel.
- For ui, each composable should have a preview to be seen and tested by the developers. The previews should consider these:
  1. For composables needing models in their input, sample models should be given to them.
  2. If a composable can be empty, it should have an empty preview too to see what happens if nothing is there.
  3. There should be another version of the previews where the models are loaded with a delay to see the shimmer effects. This is only used in the emulators.
  4. The lambda functions given to composables should not be empty and should have some sample functioning to test them whe opening the preview in the emulator. They should show the real functionality of the composable. For example if there is a subcomponent in a list that when clicked adds an item, in the preview clicking on it should add the item, even if it is a sample item.
  5. If a component has multiple states, all the states should have previews.
  6. If multiple previews can be generated using one preview composable function, it is preferred to using multiple preview function as it makes the code more DRY.
  7. There should be English and Persian version previews. Use english for only showing the default preview (number 1), and use persian for number 1 and all other ones.
  8. The previews should be grouped, as all the previews for a specific composable being in a group. This is for better navigating to the previews when viewing them.
  9. The user testing the preview should be able to see the different animations that the components subcomponents have.
- For image loading we will be using coil as our main tool.
- The navigations should not have string routes. The routes should be defined by classes and objects like it is recommended by google.
- The color pallet should be discussed before defining colors and themes.
- For other non-iu components, there should be tests written to verify their functionality.
- For testing networking, use packages that simulate networking. Delay and network errors should also be tested.
- All the commits should be in the conventional commits format.
- It is recommended to break all big ui components into subcomponents so testing the reusing them is easer and better.
- The SOLID and DRY principles should be respected.
- After adding a new feature, all tests should pass.
- Each screen will have a top-level composable that doesn't have a preview. It is the one that navigation uses. The view models are passed to this composable. It will extract all the states and functions and pass them to the screen's main composable.
- The features added should not have much dependency to each other. The reason is to make sure the 3 of us can develop features in parallel.
- Before start coding, a top level plan should be planned to define what steps should be made (some steps are done by a specific person, whilst some other are like a phase that we all do it or wny person that wants does it).
- The plan starts by defining all the criteria projects needs.
- There should be a feature graph that specifies what are the features and their dependencies. Many features, like ui screens, don't depend on each other. But they may depend on a feature that they all use. It is recommended to make these dependencies minimum.
- All the graphs, architectures, and diagrams are drawn in the eraser.io parsa's Team 2. The name of the project is "Android Project". AIs will have access to it through the eraser mcp server. The mcp server should be available and authorized for the AI to be able to work with it.
- The diagrams are only for we humans to have a better understanding of what to do. They are not 100% correct as they may be changed in the future.
- No AI token should be used in eraser.io. All the diagrams should be created manually using the APIs mcp server serves that don't use AI credit.
- It is recommended to use diagram-as-code when drawing any architecture or diagram.
- When the diagram-as-codes are drawn, if there is access to changing the component positions, the component positions should be arranged in a way that the diagram looks clean and organized.
- All the processes and graphs should be shown in eraser.
- All the ERDs should be shown in eraser.
- For phase 1, these are our tasks:
  - Parsa: Defining the project criteria, Starting the base code for project, drawing plan diagrams, starting the ui development.
  - Bagher: Working on PocketBase, defining ERDs.
  - Sina: Learning how to define the player service, define AI related docs.
- The chat feature is in the last phases.
- The Android Skills (https://github.com/android/skills) should be installed before using the AI for coding in the project.
- The AI should consider all the criteria from this document and the Android Skills.
- If there is a conflict in anything between android skills and this document, the AI should ask the developer which one to choose. In the case on conflicts, the AI should not take actions by their own.
- Before start coding, this document should be checked with Android Skills to find potential conflicts and fix them.