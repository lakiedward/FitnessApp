from pathlib import Path
path = Path(r"c:\Users\lakie\StudioProjects\FitnessApp\app\src\main\java\com\example\fitnessapp\pages\home\TrainingDetailScreen.kt")
text = path.read_text(encoding='utf-8')
idx = text.find('test_tag_description_card')
print(text[idx-120:idx+200])
