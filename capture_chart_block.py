from pathlib import Path
path = Path(r"c:\Users\lakie\StudioProjects\FitnessApp\app\src\main\java\com\example\fitnessapp\pages\home\TrainingDetailScreen.kt")
text = path.read_text(encoding='utf-8')
start = text.find("item {\n                                Card(\n                                    modifier = Modifier\n                                        .fillMaxWidth()\n                                        .testTag(stringResource(R.string.test_tag_workout_structure_card))")
end = text.find("                                }\n                            }\n\n                            // Empty state for missing workout structure", start)
print(repr(text[start:end]))
