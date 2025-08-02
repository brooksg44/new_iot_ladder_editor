# New IOT Ladder Editor - Nested Maps Approach

A modern Clojure implementation of an IOT Ladder Diagram editor using the **nested maps approach** for data representation and **cljfx** for the user interface.

## Features

- **Nested Maps Data Structure**: Clean, functional approach using nested maps for ladder logic representation
- **Correct ASCII Rendering**: Produces properly formatted ladder diagrams like the working ladder-diagram-clj project
- **IL to Ladder Conversion**: Converts Instruction List (IL) code to ladder diagrams
- **Modern GUI**: Built with cljfx (JavaFX) for a responsive user interface
- **Examples Included**: Three-wire control and other common ladder patterns

## Data Structure

This project uses the **nested maps approach** (#1) for representing ladder logic:

```clojure
{:type :ladder
 :rungs
 [{:type :rung
   :elements
   [{:type :contact
     :name "Stop"
     :normally-closed? true
     :ascii-art "─┤/├─"
     :width 5}
    {:type :branch
     :rungs
     [{:type :rung
       :elements
       [{:type :contact
         :name "Start"
         :normally-closed? false
         :ascii-art "─┤ ├─"
         :width 5}]}
      {:type :rung
       :elements
       [{:type :contact
         :name "Motor"
         :normally-closed? false
         :ascii-art "─┤ ├─"
         :width 5}]}]}
    {:type :coil
     :name "Motor"
     :normally-closed? false
     :ascii-art "─( )"
     :width 4}]}]}
```

## Installation and Usage

### Prerequisites

- Java 11 or later
- Clojure CLI tools

### Running the Application

```bash
# Clone/copy the project files
cd new_iot_ladder_editor

# Run in GUI mode (default)
clj -M:run

# Run in CLI mode to see ASCII examples
clj -M:run --cli
```

### CLI Mode Output

The CLI mode demonstrates the correct ASCII rendering:

```
=== New IOT Ladder Editor - Nested Maps Approach ===

=== Simple Two-Element Rung ===
█
█─   Start      Motor 
█────┤ ├─────────( )
█

=== Three-Wire Control Example ===
█
█─   Stop              Start      Motor 
█────┤/├───┬───────────┤ ├────────( )
█─         │                       
█─         │    Motor              
█─         └────┤ ├─────────────────
█
```

## Key Improvements

### 1. Correct Data Structure
- Uses pure nested maps (no records/deftypes)
- Clean, functional approach
- Easy to inspect and modify in REPL

### 2. Proper ASCII Rendering
- Follows the working ladder-diagram-clj rendering logic
- Correct branch formatting with ┬, ├, └, │ characters
- Proper spacing and alignment

### 3. Better IL Conversion
- Improved parsing of IL instructions
- Logical grouping into rungs based on LD/ST patterns
- Error handling and validation

### 4. Modern UI
- Built with cljfx for modern JavaFX interface
- Clean, responsive design
- Real-time conversion and preview

## API Examples

### Programmatic Usage

```clojure
(require '[ladder-editor.ladder :as ladder]
         '[ladder-editor.render :as render]
         '[ladder-editor.converter :as converter])

;; Create a simple ladder
(def simple-ladder
  (ladder/ladder
   (ladder/rung
    (ladder/normally-open-contact "Start")
    (ladder/normal-coil "Motor"))))

;; Render as ASCII
(println (render/render-ladder simple-ladder))

;; Convert IL to ladder
(def il-result (converter/convert-il-text "LD %I0.0\nST %Q0.0"))
(when (:success il-result)
  (println (render/render-ladder (:result il-result))))
```

### Three-Wire Control Example

```clojure
;; Create three-wire control
(def three-wire
  (ladder/ladder
   (ladder/rung
    (ladder/normally-closed-contact "Stop")
    (ladder/branch
     (ladder/rung (ladder/normally-open-contact "Start"))
     (ladder/rung (ladder/normally-open-contact "Motor")))
    (ladder/normal-coil "Motor"))))

(println (render/render-ladder three-wire))
```

## Project Structure

```
new_iot_ladder_editor/
├── deps.edn                    # Project dependencies
├── README.md                   # This file
├── src/ladder_editor/
│   ├── core.clj               # Main entry point
│   ├── ladder.clj             # Nested maps data structures
│   ├── render.clj             # ASCII rendering engine
│   ├── converter.clj          # IL to ladder conversion
│   └── ui.clj                 # cljfx GUI interface
└── test/ladder_editor/
    └── core_test.clj          # Test suite
```

## Comparison with Original Projects

### vs. Original iot_ladder_editor
- ✅ **Fixed**: Incorrect LD diagram output
- ✅ **Improved**: Better data structure (nested maps vs mixed approaches)
- ✅ **Enhanced**: Proper ASCII rendering
- ✅ **Maintained**: cljfx GUI interface

### vs. ladder-diagram-clj
- ✅ **Adopted**: Correct ASCII rendering approach
- ✅ **Simplified**: Pure nested maps instead of records/protocols
- ✅ **Added**: IL conversion capability
- ✅ **Enhanced**: Modern GUI with cljfx

## Testing

```bash
# Run tests
clj -M:test

# Run in REPL for interactive testing
clj
user=> (require '[ladder-editor.core :as core])
user=> (core/quick-test)
user=> (core/demo-ascii-rendering)
```

## License

Same as original projects - Eclipse Public License 2.0

## Contributing

This project demonstrates the correct implementation approach for ladder diagram editors in Clojure using:
1. Nested maps for clean data representation
2. Functional rendering approach
3. Modern UI with cljfx
4. Proper ASCII formatting

Feel free to extend with additional features or ladder elements!
