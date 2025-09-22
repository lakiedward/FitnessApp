from pathlib import Path
path = Path(r"c:\Users\lakie\StudioProjects\FitnessApp\app\src\main\java\com\example\fitnessapp\pages\home\TrainingDetailScreen.kt")
text = path.read_text(encoding='utf-8')
start_marker = "                                item {\n                                    Card(\n                                        modifier = Modifier\n                                            .fillMaxWidth()\n                                            .testTag(stringResource(R.string.test_tag_metrics_card))"
end_marker = "                                }\n                            }\n\n                            // Display the complete description"
start = text.index(start_marker)
end = text.index(end_marker, start)
print(text[start:end])
