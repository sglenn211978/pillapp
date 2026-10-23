package com.sglenn.dosepulse.data

object ClinicalSeeds {
    val items = listOf(
        // Cardiology & Blood Pressure
        MedicationReference(
            name = "Lisinopril",
            category = "Cardiology & Blood Pressure",
            commonDosages = listOf("5 mg", "10 mg", "20 mg", "40 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take at the same time each day, with or without food. Stay hydrated.",
            clinicalWarning = "Avoid potassium supplements or salt substitutes unless advised by doctor.",
            suggestedColorHex = 0xFF00796B
        ),
        MedicationReference(
            name = "Amlodipine",
            category = "Cardiology & Blood Pressure",
            commonDosages = listOf("2.5 mg", "5 mg", "10 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take once daily with or without food. Avoid excessive grapefruit products.",
            clinicalWarning = "May cause mild lower leg or ankle swelling. Report severe dizziness.",
            suggestedColorHex = 0xFF0284C7
        ),
        MedicationReference(
            name = "Losartan",
            category = "Cardiology & Blood Pressure",
            commonDosages = listOf("25 mg", "50 mg", "100 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take daily with a full glass of water. Can be taken with or without food.",
            clinicalWarning = "Do not take if pregnant. Avoid high potassium foods in excess.",
            suggestedColorHex = 0xFF0D9488
        ),
        MedicationReference(
            name = "Metoprolol Succinate",
            category = "Cardiology & Blood Pressure",
            commonDosages = listOf("25 mg", "50 mg", "100 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take with or immediately following a meal. Swallow whole.",
            clinicalWarning = "Do not abruptly discontinue without consulting your physician.",
            suggestedColorHex = 0xFF0369A1
        ),
        MedicationReference(
            name = "Hydrochlorothiazide (HCTZ)",
            category = "Cardiology & Blood Pressure",
            commonDosages = listOf("12.5 mg", "25 mg", "50 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take in the morning to prevent nighttime urination.",
            clinicalWarning = "Can increase sensitivity to sunlight. Use sunscreen outdoors.",
            suggestedColorHex = 0xFF0284C7
        ),
        MedicationReference(
            name = "Carvedilol",
            category = "Cardiology & Blood Pressure",
            commonDosages = listOf("3.125 mg", "6.25 mg", "12.5 mg", "25 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Always take with food to minimize the risk of low blood pressure.",
            clinicalWarning = "Take your pulse regularly. Report heart rate below 50 bpm.",
            suggestedColorHex = 0xFF047857
        ),

        // Cholesterol & Lipids
        MedicationReference(
            name = "Atorvastatin (Lipitor)",
            category = "Cholesterol & Lipids",
            commonDosages = listOf("10 mg", "20 mg", "40 mg", "80 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take once daily in the evening or bedtime with or without food.",
            clinicalWarning = "Avoid large amounts of grapefruit juice. Report unexplained muscle aches.",
            suggestedColorHex = 0xFF6366F1
        ),
        MedicationReference(
            name = "Rosuvastatin (Crestor)",
            category = "Cholesterol & Lipids",
            commonDosages = listOf("5 mg", "10 mg", "20 mg", "40 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take at any time of day, with or without food.",
            clinicalWarning = "Contact doctor promptly if you experience severe muscle soreness.",
            suggestedColorHex = 0xFF4F46E5
        ),
        MedicationReference(
            name = "Simvastatin",
            category = "Cholesterol & Lipids",
            commonDosages = listOf("10 mg", "20 mg", "40 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take once daily strictly in the evening.",
            clinicalWarning = "High potential for drug interactions. Check before starting new medications.",
            suggestedColorHex = 0xFF4338CA
        ),

        // Diabetes & Endocrine
        MedicationReference(
            name = "Metformin",
            category = "Diabetes & Endocrine",
            commonDosages = listOf("500 mg", "850 mg", "1000 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Always take with meals to reduce stomach upset and nausea.",
            clinicalWarning = "Avoid excessive alcohol intake. Discontinue before radiographic contrast dyes.",
            suggestedColorHex = 0xFFD97706
        ),
        MedicationReference(
            name = "Levothyroxine (Synthroid)",
            category = "Diabetes & Endocrine",
            commonDosages = listOf("25 mcg", "50 mcg", "75 mcg", "100 mcg", "125 mcg"),
            defaultForm = "tablet",
            intakeGuidance = "Take first thing in the morning on an empty stomach with water, 30-60 min before breakfast.",
            clinicalWarning = "Separate from calcium, iron, or antacids by at least 4 hours.",
            suggestedColorHex = 0xFFB45309
        ),
        MedicationReference(
            name = "Glipizide",
            category = "Diabetes & Endocrine",
            commonDosages = listOf("5 mg", "10 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take 30 minutes before your first main meal of the day.",
            clinicalWarning = "Watch for symptoms of hypoglycemia (shakiness, sweating, fast heartbeat).",
            suggestedColorHex = 0xFFF59E0B
        ),
        MedicationReference(
            name = "Empagliflozin (Jardiance)",
            category = "Diabetes & Endocrine",
            commonDosages = listOf("10 mg", "25 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take in the morning with or without food. Drink plenty of water.",
            clinicalWarning = "Ensure good personal hygiene to avoid urinary tract infections.",
            suggestedColorHex = 0xFFEA580C
        ),

        // Mental Health & Neurology
        MedicationReference(
            name = "Sertraline (Zoloft)",
            category = "Mental Health & Neurology",
            commonDosages = listOf("25 mg", "50 mg", "100 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take once daily in the morning or evening with food to avoid nausea.",
            clinicalWarning = "Takes 2-4 weeks for full clinical therapeutic effect. Do not stop suddenly.",
            suggestedColorHex = 0xFF8B5CF6
        ),
        MedicationReference(
            name = "Escitalopram (Lexapro)",
            category = "Mental Health & Neurology",
            commonDosages = listOf("5 mg", "10 mg", "20 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take once daily at the same time, with or without food.",
            clinicalWarning = "Avoid non-steroidal anti-inflammatory drugs without physician guidance.",
            suggestedColorHex = 0xFF7C3AED
        ),
        MedicationReference(
            name = "Bupropion XL (Wellbutrin)",
            category = "Mental Health & Neurology",
            commonDosages = listOf("150 mg", "300 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take in the morning to prevent insomnia. Swallow whole, never crush.",
            clinicalWarning = "Do not take if you have a history of seizures or eating disorders.",
            suggestedColorHex = 0xFF6D28D9
        ),
        MedicationReference(
            name = "Duloxetine (Cymbalta)",
            category = "Mental Health & Neurology",
            commonDosages = listOf("20 mg", "30 mg", "60 mg"),
            defaultForm = "capsule",
            intakeGuidance = "Take with or without food. Swallow capsule whole.",
            clinicalWarning = "Do not consume heavy alcohol while taking duloxetine.",
            suggestedColorHex = 0xFF9333EA
        ),
        MedicationReference(
            name = "Gabapentin (Neurontin)",
            category = "Mental Health & Neurology",
            commonDosages = listOf("100 mg", "300 mg", "600 mg", "800 mg"),
            defaultForm = "capsule",
            intakeGuidance = "Take with water. If dividing a tablet, take the other half at next dose.",
            clinicalWarning = "May cause significant drowsiness. Do not drive until you know its effect.",
            suggestedColorHex = 0xFFA855F7
        ),

        // Gastrointestinal
        MedicationReference(
            name = "Omeprazole (Prilosec)",
            category = "Gastrointestinal",
            commonDosages = listOf("20 mg", "40 mg"),
            defaultForm = "capsule",
            intakeGuidance = "Take 30-60 minutes before breakfast on an empty stomach.",
            clinicalWarning = "Swallow whole; do not chew or crush delayed-release granules.",
            suggestedColorHex = 0xFF14B8A6
        ),
        MedicationReference(
            name = "Pantoprazole (Protonix)",
            category = "Gastrointestinal",
            commonDosages = listOf("20 mg", "40 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take 30 minutes before morning meal. Swallow tablet whole.",
            clinicalWarning = "Long term use should be monitored by your prescribing physician.",
            suggestedColorHex = 0xFF0D9488
        ),
        MedicationReference(
            name = "Famotidine (Pepcid)",
            category = "Gastrointestinal",
            commonDosages = listOf("10 mg", "20 mg", "40 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take 15-60 minutes before eating food that causes heartburn.",
            clinicalWarning = "Limit caffeine and acidic foods to improve effectiveness.",
            suggestedColorHex = 0xFF059669
        ),
        MedicationReference(
            name = "Ondansetron (Zofran)",
            category = "Gastrointestinal",
            commonDosages = listOf("4 mg", "8 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Place orally disintegrating tablet on tongue and allow to dissolve.",
            clinicalWarning = "May cause mild headache or constipation.",
            suggestedColorHex = 0xFF10B981
        ),

        // Respiratory & Allergy
        MedicationReference(
            name = "Albuterol Inhaler (Ventolin/ProAir)",
            category = "Respiratory & Allergy",
            commonDosages = listOf("90 mcg (2 puffs)"),
            defaultForm = "inhaler",
            intakeGuidance = "Shake well before each spray. Wait 1 minute between puffs if taking 2.",
            clinicalWarning = "Quick-relief rescue inhaler. Seek emergency care if symptoms do not improve.",
            suggestedColorHex = 0xFF06B6D4
        ),
        MedicationReference(
            name = "Fluticasone Propionate (Flonase)",
            category = "Respiratory & Allergy",
            commonDosages = listOf("50 mcg (1-2 sprays/nostril)"),
            defaultForm = "drops",
            intakeGuidance = "Blow nose gently before use. Aim spray slightly away from center septum.",
            clinicalWarning = "Takes several days of consistent use for peak symptom relief.",
            suggestedColorHex = 0xFF0891B2
        ),
        MedicationReference(
            name = "Montelukast (Singulair)",
            category = "Respiratory & Allergy",
            commonDosages = listOf("10 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take once daily in the evening with or without food.",
            clinicalWarning = "Report any unusual mood changes or vivid dreams to your doctor.",
            suggestedColorHex = 0xFF0E7490
        ),
        MedicationReference(
            name = "Cetirizine (Zyrtec)",
            category = "Respiratory & Allergy",
            commonDosages = listOf("5 mg", "10 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take once daily. Can be taken with or without food.",
            clinicalWarning = "May cause mild drowsiness in some individuals.",
            suggestedColorHex = 0xFF0284C7
        ),

        // Antibiotics & Anti-infectives
        MedicationReference(
            name = "Amoxicillin",
            category = "Antibiotics & Anti-infectives",
            commonDosages = listOf("250 mg", "500 mg", "875 mg"),
            defaultForm = "capsule",
            intakeGuidance = "Take at evenly spaced intervals with food or milk to reduce stomach upset.",
            clinicalWarning = "Finish entire prescribed course even if symptoms resolve completely.",
            suggestedColorHex = 0xFFE11D48
        ),
        MedicationReference(
            name = "Azithromycin (Z-Pak)",
            category = "Antibiotics & Anti-infectives",
            commonDosages = listOf("250 mg", "500 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take with or without food at the same scheduled time each day.",
            clinicalWarning = "Do not take aluminum or magnesium antacids at the exact same time.",
            suggestedColorHex = 0xFFBE123C
        ),
        MedicationReference(
            name = "Cephalexin (Keflex)",
            category = "Antibiotics & Anti-infectives",
            commonDosages = listOf("250 mg", "500 mg"),
            defaultForm = "capsule",
            intakeGuidance = "Take with plenty of fluids every 6 or 12 hours as prescribed.",
            clinicalWarning = "Report any severe diarrhea or allergic rashes immediately.",
            suggestedColorHex = 0xFF9F1239
        ),
        MedicationReference(
            name = "Doxycycline Hyclate",
            category = "Antibiotics & Anti-infectives",
            commonDosages = listOf("50 mg", "100 mg"),
            defaultForm = "capsule",
            intakeGuidance = "Take with a full 8oz glass of water. Remain upright for 30 minutes after.",
            clinicalWarning = "Causes significant photosensitivity. Avoid tanning beds and harsh sun.",
            suggestedColorHex = 0xFF881337
        ),

        // Pain, Inflammation & Arthritis
        MedicationReference(
            name = "Ibuprofen (Advil/Motrin)",
            category = "Pain, Inflammation & Arthritis",
            commonDosages = listOf("200 mg", "400 mg", "600 mg", "800 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Always take with food, milk, or a full glass of water.",
            clinicalWarning = "Do not exceed maximum daily dosage. Caution with stomach ulcers.",
            suggestedColorHex = 0xFFF97316
        ),
        MedicationReference(
            name = "Acetaminophen (Tylenol)",
            category = "Pain, Inflammation & Arthritis",
            commonDosages = listOf("325 mg", "500 mg (Extra Strength)", "650 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take with or without food. Stay hydrated.",
            clinicalWarning = "Do not exceed 3,000 mg in 24 hours. Check other meds for acetaminophen.",
            suggestedColorHex = 0xFFEA580C
        ),
        MedicationReference(
            name = "Naproxen (Aleve)",
            category = "Pain, Inflammation & Arthritis",
            commonDosages = listOf("220 mg", "375 mg", "500 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take with food or milk to prevent gastrointestinal irritation.",
            clinicalWarning = "Long-acting NSAID; do not combine with ibuprofen without doctor consent.",
            suggestedColorHex = 0xFFC2410C
        ),
        MedicationReference(
            name = "Meloxicam (Mobic)",
            category = "Pain, Inflammation & Arthritis",
            commonDosages = listOf("7.5 mg", "15 mg"),
            defaultForm = "tablet",
            intakeGuidance = "Take once daily with food or milk.",
            clinicalWarning = "Monitor blood pressure periodically while taking daily NSAIDs.",
            suggestedColorHex = 0xFF9A3412
        ),

        // Vitamins & Supplements
        MedicationReference(
            name = "Vitamin D3 (Cholecalciferol)",
            category = "Vitamins & Supplements",
            commonDosages = listOf("1000 IU", "2000 IU", "5000 IU", "50,000 IU (weekly)"),
            defaultForm = "capsule",
            intakeGuidance = "Take with a meal containing dietary fat for maximum absorption.",
            clinicalWarning = "High doses should be guided by periodic blood 25(OH)D lab tests.",
            suggestedColorHex = 0xFF10B981
        ),
        MedicationReference(
            name = "Vitamin B12 (Methylcobalamin)",
            category = "Vitamins & Supplements",
            commonDosages = listOf("500 mcg", "1000 mcg", "2500 mcg"),
            defaultForm = "tablet",
            intakeGuidance = "Sublingual tablets dissolve under tongue. Best taken in morning.",
            clinicalWarning = "Essential for red blood cell synthesis and nerve sheath function.",
            suggestedColorHex = 0xFF059669
        ),
        MedicationReference(
            name = "Ferrous Sulfate (Iron)",
            category = "Vitamins & Supplements",
            commonDosages = listOf("65 mg Elemental Iron (325 mg)"),
            defaultForm = "tablet",
            intakeGuidance = "Best absorbed on empty stomach with Vitamin C (orange juice). Take with food if nauseous.",
            clinicalWarning = "Do not take within 2 hours of dairy, calcium, or antacids. Stools may turn dark.",
            suggestedColorHex = 0xFF047857
        ),
        MedicationReference(
            name = "Omega-3 Fish Oil (EPA/DHA)",
            category = "Vitamins & Supplements",
            commonDosages = listOf("1000 mg", "1200 mg"),
            defaultForm = "capsule",
            intakeGuidance = "Take with your largest meal to prevent fishy aftertaste.",
            clinicalWarning = "Consult physician if taking concurrent blood thinners.",
            suggestedColorHex = 0xFF00796B
        ),
        MedicationReference(
            name = "Magnesium Glycinate",
            category = "Vitamins & Supplements",
            commonDosages = listOf("200 mg", "400 mg"),
            defaultForm = "capsule",
            intakeGuidance = "Take 30-60 minutes before bedtime with water.",
            clinicalWarning = "Gentle on the stomach compared to magnesium citrate or oxide.",
            suggestedColorHex = 0xFF0F766E
        )
    )
}
