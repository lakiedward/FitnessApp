from pathlib import Path
path = Path(r"c:\Users\lakie\StudioProjects\FitnessApp\app\src\main\java\com\example\fitnessapp\pages\home\TrainingDetailScreen.kt")
text = path.read_text(encoding='utf-8')
print('carriage returns:', text.count('\r'))
