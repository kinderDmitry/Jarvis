# JARVIS UI direction — v5.2

Use the supplied reference image as the visual source of truth, but implement it as a real responsive Android interface rather than a collage.

## Target
- Premium futuristic JARVIS assistant, not a utility dashboard.
- Black/deep-navy background with bright cyan/electric-blue neon accents.
- One dominant central AI Core with a clean helmet/face mark and thin concentric HUD rings.
- Large visual hierarchy: logo -> core -> status -> conversation -> input.
- Very little copy. Never fill the screen with explanatory paragraphs.
- Rounded dark panels, thin blue borders, restrained glow.
- No flattened grids of tiny buttons.
- No decorative text pretending to be functionality.
- The microphone is the primary action.
- The screen must remain readable on narrow phones and scale vertically without fixed-width stretching.

## Motion
- Core rings rotate slowly.
- Waveform reacts to LISTENING/SPEAKING states.
- Animations are Canvas-based and hardware accelerated.
- No bitmap-heavy blur, particle systems, or continuous software shadows.

## States
READY / LISTENING / THINKING / EXECUTING / SPEAKING / ERROR

The state is shown as one compact line. The assistant should feel alive through motion, not through extra text.

## Interaction
- Tap the core or microphone to talk.
- Type in one compact field when needed.
- Conversation appears only after the user interacts.
- System-assistant setup is a single compact card.
- Wake-word control is a single compact action.
- Settings are behind one gear icon.
